from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Dict, List, Any
import pickle
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
import os

from pytorch_forecasting import TemporalFusionTransformer, TimeSeriesDataSet

app = FastAPI(title="成都天气 WMO 预测验证 API", version="1.0")

# 使用相对路径，方便服务器部署
MODEL_DIR = os.getenv("MODEL_DIR", os.path.join(os.path.dirname(__file__), "cd", "weathercode", "oneDay"))

model = TemporalFusionTransformer.load_from_checkpoint(
    os.path.join(MODEL_DIR, "cd_tft_weather_code.ckpt")
)
print("✅ TFT模型加载成功")

with open(os.path.join(MODEL_DIR, "scaler_weather_code.pkl"), "rb") as f:
    scaler_tuple = pickle.load(f)
    scaler = scaler_tuple[0]
    valid_cols = scaler_tuple[1]
print("✅ Scaler 和 valid_cols 已加载")

with open(os.path.join(MODEL_DIR, "wmo_mapping.pkl"), "rb") as f:
    wmo_data = pickle.load(f)
    wmo_to_idx = wmo_data[0]
    idx_to_wmo = wmo_data[1]
print("✅ WMO映射已加载")

COLUMN_MAPPING = {
    "temperature_2m": "temp",
    "relative_humidity_2m": "rh",
    "dew_point_2m": "dew",
    "precipitation": "precip",
    "rain": "rain",
    "snowfall": "snow",
    "apparent_temperature": "app_temp",
    "weather_code": "weather_code",
    "pressure_msl": "p_msl",
    "surface_pressure": "surf_p",
    "cloud_cover": "cloud",
    "wind_speed_10m": "wind10",
    "wind_direction_10m": "dir10",
    "wind_gusts_10m": "gust10",
    "soil_temperature_0_to_7cm": "soil_t0",
    "snow_depth": "snow_depth",
    "cloud_cover_low": "cloud_low",
    "cloud_cover_mid": "cloud_mid",
    "cloud_cover_high": "cloud_high",
    "et0_fao_evapotranspiration": "et0",
    "vapour_pressure_deficit": "vpd",
    "wind_speed_100m": "wind100",
    "wind_direction_100m": "dir100",
    "soil_temperature_7_to_28cm": "soil_t7",
    "soil_moisture_0_to_7cm": "soil_m0",
    "soil_moisture_7_to_28cm": "soil_m7"
}


class WeatherDataRequest(BaseModel):
    hourly: Dict[str, List[Any]]


def prepare_dataframe(request: WeatherDataRequest):
    data_dict = {"time": pd.to_datetime(request.hourly["time"])}

    for long_name, short_name in COLUMN_MAPPING.items():
        if long_name in request.hourly:
            data_dict[short_name] = request.hourly[long_name]
        else:
            default_value = 0.0 if short_name != "weather_code" else 3
            data_dict[short_name] = [default_value] * len(request.hourly["time"])

    df = pd.DataFrame(data_dict).sort_values("time").reset_index(drop=True)

    df["time_idx"] = np.arange(len(df))
    df["group"] = 0

    df["weather_code_idx"] = df["weather_code"].map(wmo_to_idx).fillna(0).astype(int)

    if scaler is not None and valid_cols:
        df[valid_cols] = scaler.transform(df[valid_cols])

    return df


@app.post("/validate/wmo")
def validate_wmo_prediction(request: WeatherDataRequest):
    try:
        df = prepare_dataframe(request)
        print(f"✅ 成功构建DataFrame，形状: {df.shape}")

        start_date = datetime(2026, 1, 8, 0, 0)
        current = start_date
        all_results = []

        while current <= df["time"].max():
            try:
                idx = df[df["time"] == current].index[0]
            except IndexError:
                current += timedelta(hours=24)
                continue

            start_idx = idx - 168
            if start_idx < 0:
                current += timedelta(hours=24)
                continue

            full_df = df.iloc[start_idx:idx + 24].copy()
            if len(full_df) != 192:
                current += timedelta(hours=24)
                continue

            try:
                predict_dataset = TimeSeriesDataSet(
                    full_df,
                    time_idx="time_idx",
                    target="weather_code_idx",
                    group_ids=["group"],
                    min_encoder_length=168,
                    max_encoder_length=168,
                    min_prediction_length=24,
                    max_prediction_length=24,
                    static_categoricals=[],
                    static_reals=[],
                    time_varying_known_categoricals=[],
                    time_varying_known_reals=['wind10', 'dir10', 'p_msl', 'cloud', 'wind100', 'dir100'],
                    time_varying_unknown_categoricals=[],
                    time_varying_unknown_reals=['temp', 'rh', 'dew', 'precip', 'rain', 'snow', 'app_temp', 'surf_p', 'gust10', 'soil_t0', 'snow_depth', 'cloud_low', 'cloud_mid', 'cloud_high', 'et0', 'vpd', 'soil_t7', 'soil_m0', 'soil_m7', 'weather_code_idx'],
                    target_normalizer=None,
                    add_relative_time_idx=True,
                    add_target_scales=False,
                    add_encoder_length=True,
                    allow_missing_timesteps=False,
                    predict_mode=True,
                    min_prediction_idx=0
                )

                predictions = model.predict(predict_dataset, mode="prediction", return_x=False)
                
                if predictions.dim() == 1:
                    pred_indices = predictions.numpy()
                elif predictions.dim() == 2:
                    pred_indices = predictions.squeeze(0).numpy()
                else:
                    pred_indices = predictions.numpy()
                
                if pred_indices.ndim == 0:
                    pred_wmo = [int(idx_to_wmo.get(int(pred_indices.item()), -1))]
                else:
                    pred_wmo = [int(idx_to_wmo.get(int(idx), -1)) for idx in pred_indices]
                
                real_wmo = full_df.iloc[168:]["weather_code"].values.astype(int)

                if len(pred_wmo) == 1:
                    pred_wmo = pred_wmo * 24

                print(f"\n📅 预测日期: {current.strftime('%Y-%m-%d')}")
                print("-" * 60)
                correct = 0
                for i in range(min(24, len(pred_wmo))):
                    pred = pred_wmo[i]
                    real = real_wmo[i]
                    match = "✅" if pred == real else "❌"
                    print(f"{i:02d}:00 | 预测: {pred:2d} | 真实: {real:2d} | {match}")
                    if pred == real:
                        correct += 1
                accuracy = correct / 24 * 100
                print(f"🎯 准确率: {correct}/24 = {accuracy:.1f}%")

                all_results.append({
                    "date": current.strftime('%Y-%m-%d'),
                    "predictions": pred_wmo[:24],
                    "real_values": real_wmo.tolist(),
                    "accuracy": accuracy,
                    "correct_count": correct
                })

            except Exception as e:
                print(f"❌ 预测失败: {str(e)}")
                import traceback
                traceback.print_exc()
                current += timedelta(hours=24)
                continue

            current += timedelta(hours=24)

        return {
            "status": "success",
            "message": "预测完成",
            "results": all_results
        }

    except Exception as e:
        print(f"❌ 验证失败: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/predict/wmo")
def predict_wmo(request: WeatherDataRequest):
    try:
        df = prepare_dataframe(request)
        print(f"✅ 成功构建DataFrame，形状: {df.shape}")

        if len(df) < 168:
            raise HTTPException(
                status_code=400,
                detail=f"数据不足，需要至少168小时的历史数据，当前只有{len(df)}小时"
            )

        full_df = df.iloc[-192:].copy()
        full_df["time_idx"] = np.arange(192)

        try:
            predict_dataset = TimeSeriesDataSet(
                full_df,
                time_idx="time_idx",
                target="weather_code_idx",
                group_ids=["group"],
                min_encoder_length=168,
                max_encoder_length=168,
                min_prediction_length=24,
                max_prediction_length=24,
                static_categoricals=[],
                static_reals=[],
                time_varying_known_categoricals=[],
                time_varying_known_reals=['wind10', 'dir10', 'p_msl', 'cloud', 'wind100', 'dir100'],
                time_varying_unknown_categoricals=[],
                time_varying_unknown_reals=['temp', 'rh', 'dew', 'precip', 'rain', 'snow', 'app_temp', 'surf_p', 'gust10', 'soil_t0', 'snow_depth', 'cloud_low', 'cloud_mid', 'cloud_high', 'et0', 'vpd', 'soil_t7', 'soil_m0', 'soil_m7', 'weather_code_idx'],
                target_normalizer=None,
                add_relative_time_idx=True,
                add_target_scales=False,
                add_encoder_length=True,
                allow_missing_timesteps=False,
                predict_mode=True,
                min_prediction_idx=0
            )

            predictions = model.predict(predict_dataset, mode="prediction", return_x=False)
            
            if predictions.dim() == 1:
                pred_indices = predictions.numpy()
            elif predictions.dim() == 2:
                pred_indices = predictions.squeeze(0).numpy()
            else:
                pred_indices = predictions.numpy()
            
            if pred_indices.ndim == 0:
                pred_wmo = [int(idx_to_wmo.get(int(pred_indices.item()), -1))]
            else:
                pred_wmo = [int(idx_to_wmo.get(int(idx), -1)) for idx in pred_indices]
            
            if len(pred_wmo) == 1:
                pred_wmo = pred_wmo * 24

            print(f"\n📅 预测未来24小时天气代码")
            print("-" * 60)
            for i in range(min(24, len(pred_wmo))):
                print(f"{i:02d}:00 | 预测: {pred_wmo[i]:2d}")

            return {
                "status": "success",
                "predictions": pred_wmo[:24]
            }

        except Exception as e:
            print(f"❌ 预测失败: {str(e)}")
            import traceback
            traceback.print_exc()
            raise HTTPException(status_code=500, detail=str(e))

    except HTTPException:
        raise
    except Exception as e:
        print(f"❌ 预测失败: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
