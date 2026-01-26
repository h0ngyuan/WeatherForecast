package com.wf.controller;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.wf.object.query.LoginQuery;
import com.wf.service.WxService;
import org.springblade.core.tool.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/wx")
public class WxController {

    @Autowired
    private WxService wxService;


    @PostMapping("/login")
    public R<String> wxLogin(@RequestBody() LoginQuery query) {
        return R.success(wxService.login(query));
    }

    @GetMapping("/hi")
    public R hi(){
        return R.success("hi");
    }
}
