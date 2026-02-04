本指南演示如何使用 Zilliz Cloud 集群高效执行语义检索的相关操作。

如下步骤假设您已经创建了 Zilliz Cloud 集群，获取了可以访问该集群的 API Key 或鉴权凭据，并安装了相关 SDK。

建立连接
获取集群凭证或 API 密钥后，您可以通过以下示例代码连接到集群。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.client.ConnectConfig;

String CLUSTER_ENDPOINT = "YOUR_CLUSTER_ENDPOINT";
String TOKEN = "YOUR_CLUSTER_TOKEN";
// A valid token could be either
// - An API key, or
// - A colon-joined cluster username and password, as in `user:pass`

// 1. Connect to Milvus server
ConnectConfig connectConfig = ConnectConfig.builder()
.uri(CLUSTER_ENDPOINT)
.token(TOKEN)
.build();

MilvusClientV2 client = new MilvusClientV2(connectConfig);

创建 collection
在 Zilliz Cloud， 您需要将向量数据存储到 collection 中。同一个 collection 中的向量数据具有相同的维度和相似度测量指标。

在创建 collection 时，您需要详细定义每个字段的属性，如字段名称、数据类型和其他属性。另外，您还可以选择为需要加速检索的字段创建索引。其中，向量字段必须创建索引。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;

// 3.1 Create schema
CreateCollectionReq.CollectionSchema schema = client.createSchema();

// 3.2 Add fields to schema

AddFieldReq myId = AddFieldReq.builder()
.fieldName("my_id")
.dataType(DataType.Int64)
.isPrimaryKey(true)
.autoID(false)
.build();

schema.addField(myId);

AddFieldReq myVector = AddFieldReq.builder()
.fieldName("my_vector")
.dataType(DataType.FloatVector)
.dimension(5)
.build();

schema.addField(myVector);

// 3.3 Prepare index parameters
IndexParam indexParamForIdField = IndexParam.builder()
.fieldName("my_id")
.indexType(IndexParam.IndexType.STL_SORT)
.build();

IndexParam indexParamForVectorField = IndexParam.builder()
.fieldName("my_vector")
.indexType(IndexParam.IndexType.AUTOINDEX)
.metricType(IndexParam.MetricType.IP)
.build();

List<IndexParam> indexParams = new ArrayList<>();
indexParams.add(indexParamForIdField);
indexParams.add(indexParamForVectorField);

// 3.4 Create a collection with schema and index parameters
CreateCollectionReq customizedSetupReq = CreateCollectionReq.builder()
.collectionName("custom_setup")
.collectionSchema(schema)
.indexParams(indexParams)
.build();

client.createCollection(customizedSetupReq);

通过以上代码，您可以自由定义 collection 的各项属性，包括 schema 和索引参数等。

Schema

Schema 决定了 collection 的结构。除了上述代码添加的预定义字段外，您还可以启用或禁用以下功能：

Auto ID

是否自动递增 collection 的主键值。

Dynamic Field

是否使用保留 JSON 字段 $meta 来存储在 schema 中未定义的字段和字段值。

有关更多信息，请参阅了解 Schema。

索引参数

索引参数将定义 Zilliz Cloud 如何处理 collection 中的数据。您可以为字段设置特定的索引类型和度量类型。

向量字段可以选择 AUTOINDEX 作为索引类型，并采用 COSINE、L2或 IP 作为度量类型（metric_type）。

标量字段，如主键字段，整数型使用 TRIE，字符串类型使用 STL_SORT。

有关更多信息，请参阅 AUTOINDEX。

📘说明
通过上述代码创建的 collection 将自动加载（load）。如需管理非自动加载的 collection，请参阅创建 Collection。

通过 RESTful API 创建的 collection 会自动完成加载（load）。

插入数据
在准备好 collection 后，您可以参考如下示例向其中插入数据。

Python
Java
Go
NodeJS
cURL
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Arrays;
import com.alibaba.fastjson.JSONObject;

import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.InsertResp;

// 4. Insert data into the collection

// 4.1. Prepare data

List<JSONObject> insertData = Arrays.asList(
new JSONObject(Map.of("id", 0L, "vector", Arrays.asList(0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f), "color", "pink_8682")),
new JSONObject(Map.of("id", 1L, "vector", Arrays.asList(0.19886812562848388f, 0.06023560599112088f, 0.6976963061752597f, 0.2614474506242501f, 0.838729485096104f), "color", "red_7025")),
new JSONObject(Map.of("id", 2L, "vector", Arrays.asList(0.43742130801983836f, -0.5597502546264526f, 0.6457887650909682f, 0.7894058910881185f, 0.20785793220625592f), "color", "orange_6781")),
new JSONObject(Map.of("id", 3L, "vector", Arrays.asList(0.3172005263489739f, 0.9719044792798428f, -0.36981146090600725f, -0.4860894583077995f, 0.95791889146345f), "color", "pink_9298")),
new JSONObject(Map.of("id", 4L, "vector", Arrays.asList(0.4452349528804562f, -0.8757026943054742f, 0.8220779437047674f, 0.46406290649483184f, 0.30337481143159106f), "color", "red_4794")),
new JSONObject(Map.of("id", 5L, "vector", Arrays.asList(0.985825131989184f, -0.8144651566660419f, 0.6299267002202009f, 0.1206906911183383f, -0.1446277761879955f), "color", "yellow_4222")),
new JSONObject(Map.of("id", 6L, "vector", Arrays.asList(0.8371977790571115f, -0.015764369584852833f, -0.31062937026679327f, -0.562666951622192f, -0.8984947637863987f), "color", "red_9392")),
new JSONObject(Map.of("id", 7L, "vector", Arrays.asList(-0.33445148015177995f, -0.2567135004164067f, 0.8987539745369246f, 0.9402995886420709f, 0.5378064918413052f), "color", "grey_8510")),
new JSONObject(Map.of("id", 8L, "vector", Arrays.asList(0.39524717779832685f, 0.4000257286739164f, -0.5890507376891594f, -0.8650502298996872f, -0.6140360785406336f), "color", "white_9381")),
new JSONObject(Map.of("id", 9L, "vector", Arrays.asList(0.5718280481994695f, 0.24070317428066512f, -0.3737913482606834f, -0.06726932177492717f, -0.6980531615588608f), "color", "purple_4976"))
);

// 4.2. Insert data

InsertReq insertReq = InsertReq.builder()
.collectionName("custom_setup")
.data(insertData)
.build();

InsertResp res = client.insert(insertReq);

System.out.println(JSONObject.toJSON(res));

// Output:
// {"insertCnt": 10}

假设您已通过快速创建的方式完成了 collection 创建。通过以上代码：

插入的数据为字典列表，每个字典代表一条数据记录，即 entity。

每个字典包含一个名为 color 的非 schema 定义字段。

每个字典包含与预定义和动态字段相对应的键值。

📘说明
通过 RESTful API 创建的 collection 启用了 AutoID，因此插入数据时应跳过主键字段。

插入数据为异步操作。在插入数据后立即进行检索，检索结果可能为空。建议您在插入数据后等待一段时间。

相似性搜索（search）
您可以基于一条或多条向量 embedding 执行相似性搜索（search）。您还可以在搜索请求中携带过滤条件来增强搜索结果。

Python
Java
Go
NodeJS
cURL
// 8. Search with a filter expression using schema-defined fields
List<List<Float>> filteredVectorSearchData = new ArrayList<>();
filteredVectorSearchData.add(Arrays.asList(0.041732933f, 0.013779674f, -0.027564144f, -0.013061441f, 0.009748648f));

searchReq = SearchReq.builder()
.collectionName("custom_setup")
.data(filteredVectorSearchData)
.filter("4 < id < 8")
.outputFields(Arrays.asList("id"))
.topK(3)
.build();

SearchResp filteredVectorSearchRes = client.search(searchReq);

System.out.println(JSONObject.toJSON(filteredVectorSearchRes));

// Output:
// {"searchResults": [[
//     {
//         "distance": 0.08821295,
//         "id": 5,
//         "entity": {"id": 5}
//     },
//     {
//         "distance": 0.074322253,
//         "id": 6,
//         "entity": {"id": 6}
//     },
//     {
//         "distance": 0.072796463,
//         "id": 7,
//         "entity": {"id": 7}
//     }
// ]]}

输出结果为列表形式，内含三个字典类型的子列表。每个字典代表一个 entity，包括其 ID、相似距离和指定的输出字段。

您还可以在过滤表达式（filter）中加入动态字段（dynamic field）。以下代码示例中，color 是未在 schema 中定义的字段，可以通过 $meta 魔术字段的来访问，如 $meta["color"]，或像其他 schema 中已定义字段那样直接使用，如 color。

Python
Java
Go
NodeJS
cURL
// 9. Search with a filter expression using custom fields
List<List<Float>> customFilteredVectorSearchData = new ArrayList<>();
customFilteredVectorSearchData.add(Arrays.asList(0.041732933f, 0.013779674f, -0.027564144f, -0.013061441f, 0.009748648f));

searchReq = SearchReq.builder()
.collectionName("custom_setup")
.data(customFilteredVectorSearchData)
.filter("$meta[\"color\"] like \"red%\"")
.topK(3)
.outputFields(Arrays.asList("color"))
.build();

SearchResp customFilteredVectorSearchRes = client.search(searchReq);

System.out.println(JSONObject.toJSON(customFilteredVectorSearchRes));

// Output:
// {"searchResults": [[
//     {
//         "distance": 0.08821295,
//         "id": 5,
//         "entity": {"color": "yellow_4222"}
//     },
//     {
//         "distance": 0.074322253,
//         "id": 6,
//         "entity": {"color": "red_9392"}
//     },
//     {
//         "distance": 0.072796463,
//         "id": 7,
//         "entity": {"color": "grey_8510"}
//     }
// ]]}

删除 entity
您可以通过 ID 或过滤表达式删除 entity。

Delete entities by IDs.

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.response.DeleteResp;

// 13. Delete entities by IDs
DeleteReq deleteReq = DeleteReq.builder()
.collectionName("custom_setup")
.ids(Arrays.asList(0L, 1L, 2L, 3L, 4L))
.build();

DeleteResp deleteRes = client.delete(deleteReq);

System.out.println(JSONObject.toJSON(deleteRes));

// Output:
// {"deleteCnt": 5}

通过过滤表达式删除 entity

Python
Java
Go
NodeJS
cURL
// 14. Delete entities by filter
DeleteReq filterDeleteReq = DeleteReq.builder()
.collectionName("custom_setup")
.filter("id in [5, 6, 7, 8, 9]")
.build();

DeleteResp filterDeleteRes = client.delete(filterDeleteReq);

System.out.println(JSONObject.toJSON(filterDeleteRes));

// Output:
// {"deleteCnt": 5}


📘说明
目前，RESTful API 的 delete 接口暂不支持过滤表达式。

删除 collection
本指南完成后，您可以如下操作来删除 collection：

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.collection.request.DropCollectionReq;

// 15. Drop collections
DropCollectionReq dropQuickSetupParam = DropCollectionReq.builder()
.collectionName("custom_setup")
.build();

client.dropCollection(dropQuickSetupParam);

DropCollectionReq dropCustomizedSetupParam = DropCollectionReq.builder()
.collectionName("custom_setup")
.build();

client.dropCollection(dropCustomizedSetupParam);

总结
在创建 collection 前，您需要为 collection 定义 schema，和各字段的相关配置。

插入数据可能需要些时间，因此建议在插入数据后等待几秒钟再进行相似性搜索。

过滤表达式同时适用于搜索（search）和查询（query）请求，但在查询（query）请求时必须使用

基本 Vector Search
近似最近邻（ANN）Search 通过构建索引的方式对向量空间中的向量进行预排序，并在收到 Search 请求时根据索引快速定位到与查询向量相似可能性较高的子集中进行对比查询，从而提升查询效率。本节主要介绍如何使用 Milvus 进行 ANN Search 及相关的注意事项。

概述
向量搜索的实现主要依赖两类算法，一类是 k-最近邻（kNN）Search，一类是 ANN Search。kNN 算法将查询向量与向量空间中的每个向量进行比较，直到出现 k 个完全匹配的结果。尽管 kNN 搜索可以确保准确性，但十分耗时。尤其是数据量大，向量维度高时，耗时更久。

相比之下，ANN 算法会预先构建索引。并在收到 Search 请求时根据索引快速定位到与查询向量相似可能性较高的子集，然后根据请求中携带的相似度类型计算查询向量和子集中各向量的相似性，并对计算结果进行排序，从而在更短的时间内返回与查询向量相似度最高的 topK 个向量。

ANN Search 依赖预先创建的索引。选择不同的索引算法会影响搜索速度、内存使用情况和准确性。用户需要在召回率和性能之间做出取舍。为了降低用户的学习曲线，Zilliz Cloud 提供了 AUTOINDEX。通过在建立索引时分析用户数据的分布情况，使用机器学习模型自动选择检索参数，实现召回率和检索性能间的平衡。

关于 AUTOINDEX 的详细内容，可以参考本手册中的 AUTOINDEX 一节的内容。关于相似度类型，可以参考本手册中相似度类型一节的内容。本节将围绕如下话题展开讨论：

单路查询

批量查询

在 Partition中进行 ANN Search

使用 Output Fields 参数

使用 Limit 和 Offset 参数

使用 Level 参数

查看召回率

使用 Async 和 Callback 参数

单路查询
在 ANN Search 中，单路查询是指在 Search 请求中携带一个查询向量，由 Zilliz Cloud 通过预先建立的索引和请求中携带的相似度类型快速找到与查询向量最相近的 topK 个向量。

本节将演示如何在使用最简建表方式创建的 Collection 中进行单路查询。示例代码中的 Search 请示携带了一个查询向量，要求使用内积（IP）算法计算查询向量和目标向量间的相似度，并返回最相近的 3 个向量。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;

import java.util.*;

MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
.uri("YOUR_CLUSTER_ENDPOINT")
.token("YOUR_CLUSTER_TOKEN")
.build());

FloatVec queryVector = new FloatVec(new float[]{0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f});
SearchReq searchReq = SearchReq.builder()
.collectionName("quick_setup")
.data(Collections.singletonList(queryVector))
.annsField("vector")
.topK(3)
.build();

SearchResp searchResp = client.search(searchReq);

List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
for (List<SearchResp.SearchResult> results : searchResults) {
System.out.println("TopK results:");
for (SearchResp.SearchResult result : results) {
System.out.println(result);
}
}

// Output
// TopK results:
// SearchResp.SearchResult(entity={}, score=0.95944905, id=5)
// SearchResp.SearchResult(entity={}, score=0.8689616, id=1)
// SearchResp.SearchResult(entity={}, score=0.866088, id=7)

返回的结果将按相似度进行排序，与查询向量最相似的结果排在前面。度量值的大小根据相似度类型的不同呈现出不同的特点。下表展示了使用不同的相似度类型，其度量值的特点。

相似度类型

特点

取值范围

L2

较小的 L2 距离表示更高的相似性。

[0, ∞)

IP

较大的 IP 距离表示更高的相似性。

[-1, 1]

COSINE

较大的 cosine 值表示更高的相似性。

[-1, 1]

JACCARD

较小的 Jaccard 距离表示更高的相似性。

[0, 1]

HAMMING

较小的 Hamming 距离表示更高的相似性。

[0, dim(vector)]

批量查询
您也可以在 Search 请求中携带多个查询向量，Zilliz Cloud 将分别针对这两个查询向量执行 ANN Search，并返回两组查询结果。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.vector.request.SearchReq
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp

List<BaseVector> queryVectors = Arrays.asList(
new FloatVec(new float[]{0.041732933f, 0.013779674f, -0.027564144f, -0.013061441f, 0.009748648f}),
new FloatVec(new float[]{0.0039737443f, 0.003020432f, -0.0006188639f, 0.03913546f, -0.00089768134f})
);
SearchReq searchReq = SearchReq.builder()
.collectionName("quick_setup")
.data(queryVectors)
.topK(3)
.build();

SearchResp searchResp = client.search(searchReq);

List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
for (List<SearchResp.SearchResult> results : searchResults) {
System.out.println("TopK results:");
for (SearchResp.SearchResult result : results) {
System.out.println(result);
}
}

// Output
// TopK results:
// SearchResp.SearchResult(entity={}, score=0.49548206, id=1)
// SearchResp.SearchResult(entity={}, score=0.320147, id=3)
// SearchResp.SearchResult(entity={}, score=0.107413776, id=6)
// TopK results:
// SearchResp.SearchResult(entity={}, score=0.5678123, id=6)
// SearchResp.SearchResult(entity={}, score=0.32368967, id=2)
// SearchResp.SearchResult(entity={}, score=0.24108477, id=3)

主键搜索
除了可以在搜索请求中设置查询向量外，您还可以使用 Collection 中在指定字段上包含了查询向量的 Entity 的主键来进行查询。

Python
Java
NodeJS
Go
cURL
// java

在 Parition 中进行 ANN Search
如果 Collection 中存在多个按具体划分规则划分的 Partition，而且您的查询目标可以具体到其中的一个或多个 Partition。您就可以在 Search 请求中携带目标 Partition 的名称。通过减少扫描的数据量，可以显著提高搜索速度。

在以下示例代码中，假设存在一个名为 partitionA 的 Partition。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.vector.request.SearchReq
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp

FloatVec queryVector = new FloatVec(new float[]{0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f});
SearchReq searchReq = SearchReq.builder()
.collectionName("quick_setup")
.partitionNames(Collections.singletonList("partitionA"))
.data(Collections.singletonList(queryVector))
.topK(3)
.build();

SearchResp searchResp = client.search(searchReq);

List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
for (List<SearchResp.SearchResult> results : searchResults) {
System.out.println("TopK results:");
for (SearchResp.SearchResult result : results) {
System.out.println(result);
}
}

// Output
// TopK results:
// SearchResp.SearchResult(entity={}, score=0.6395302, id=13)
// SearchResp.SearchResult(entity={}, score=0.5408028, id=12)
// SearchResp.SearchResult(entity={}, score=0.49696884, id=17)

使用 Output Fields 参数
在 Zilliz Cloud 中，ANN Search 默认返回与查询向量最相近的 topK 个 Entity 的主键值 (id) 及该 Entity 与查询向量的相似度得分 (distance 或 score)。如果要求返回的每个 Entity 中都携带指定字段的值，可以在 Search 请求中指定 Output Fields (输出字段)。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.vector.request.SearchReq
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp

FloatVec queryVector = new FloatVec(new float[]{0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f});
SearchReq searchReq = SearchReq.builder()
.collectionName("quick_setup")
.data(Collections.singletonList(queryVector))
.topK(3)
.outputFields(Collections.singletonList("color"))
.build();

SearchResp searchResp = client.search(searchReq);

List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
for (List<SearchResp.SearchResult> results : searchResults) {
System.out.println("TopK results:");
for (SearchResp.SearchResult result : results) {
System.out.println(result);
}
}

// Output
// TopK results:
// SearchResp.SearchResult(entity={color=black_9955}, score=0.95944905, id=5)
// SearchResp.SearchResult(entity={color=red_7319}, score=0.8689616, id=1)
// SearchResp.SearchResult(entity={color=white_5015}, score=0.866088, id=7)

使用 Limit 和 Offset 参数
通过上面的代码示例，您可能注意到了用于控制 Search 结果中的 Entity 数量的 limit 参数。这个参数代表单次查询结果中要求包含的 Entity 的最大数量，一般称之为 topK。

如果您希望进行分页查询，可以循环发送 Search 请求，并在每次查询请求中都携带 Limit 和 Offset 参数。具体来说，可以将 Limit 参数设置为当次查询的结果中需要包含的 Entity 数量，Offset 设置为之前已经返回的所有 Entity 的数量。

下表罗列了以每次返回 100 个 Entity 的速度进行分页查询时如何设置 Limit 和 Offset 参数。

单页返回 Entity 数量（Limit）

已返回 Entity 总数量（Offset）

第 1 次

100

0

第 2 次

100

100

第 3 次

100

200

第 n 次

100

100 x (n - 1)

需要注意的是，ANN Search 单次召回 Entity 的数量为 offset 和 limit 两个参数之和，最大不超过 16,384。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.vector.request.SearchReq
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp

FloatVec queryVector = new FloatVec(new float[]{0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f});
SearchReq searchReq = SearchReq.builder()
.collectionName("quick_setup")
.data(Collections.singletonList(queryVector))
.topK(3)
.offset(10)
.build();

SearchResp searchResp = client.search(searchReq);

List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
for (List<SearchResp.SearchResult> results : searchResults) {
System.out.println("TopK results:");
for (SearchResp.SearchResult result : results) {
System.out.println(result);
}
}

// Output
// TopK results:
// SearchResp.SearchResult(entity={}, score=0.24120237, id=16)
// SearchResp.SearchResult(entity={}, score=0.22559784, id=9)
// SearchResp.SearchResult(entity={}, score=-0.09906838, id=2)

使用 Level 参数
检索调优要求根据不同的索引类型调整不同的参数。Zilliz Cloud 使用了一个统一的检索精度控制参数 level，简化了检索参数调优的过程。

该参数默认值为 1，最大值为 10。调升参数值会提高召回率，但会相对降低检索性能。通常情况下，默认的检索精度可以支撑 90% 左右的召回率，基本满足大多数场景需求。如需更高的召回率，可以尝试调升该参数。

📘说明
查询参数 Level 当前仍处于公测阶段。如果您设置了高于 5 的值而搜索结果没有变化，您的 Cluster 可能尚未支持该参数。您可以继续按照 1 - 5 的范围调节召回效果或联系 Zilliz Cloud 支持。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.vector.request.SearchReq
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp

FloatVec queryVector = new FloatVec(new float[]{0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f});
Map<String, Object> params = new HashMap<>();
params.put("level", 10);
SearchReq searchReq = SearchReq.builder()
.collectionName("quick_setup")
.data(Collections.singletonList(queryVector))
.topK(3)
.searchParams(params)
.build();

SearchResp searchResp = client.search(searchReq);

List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
for (List<SearchResp.SearchResult> results : searchResults) {
System.out.println("TopK results:");
for (SearchResp.SearchResult result : results) {
System.out.println(result);
}
}

// Output
// TopK results:
// SearchResp.SearchResult(entity={}, score=0.95944905, id=5)
// SearchResp.SearchResult(entity={}, score=0.8689616, id=1)
// SearchResp.SearchResult(entity={}, score=0.866088, id=7)

查看召回率
您还可以在调节 level 参数期间将 enable_recall_rate 参数设置为 true，以便在搜索结果中查看当前 level 值对应的召回率信息。

📘说明
查询参数 enable_recall_rate 当前仍处于公测阶段。您的集群可能尚未支持该参数。如需体验，可以联系 Zilliz Cloud 支持。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.vector.request.SearchReq
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp

FloatVec queryVector = new FloatVec(new float[]{0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f});
Map<String, Object> params = new HashMap<>();
params.put("level", 10);
params.put("enable_recall_calculation", true)
SearchReq searchReq = SearchReq.builder()
.collectionName("quick_setup")
.data(Collections.singletonList(queryVector))
.topK(3)
.searchParams(params)
.build();

SearchResp searchResp = client.search(searchReq);

List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
for (List<SearchResp.SearchResult> results : searchResults) {
System.out.println("TopK results:");
for (SearchResp.SearchResult result : results) {
System.out.println(result);
}
}

// Output
// TopK results:
// SearchResp.SearchResult(entity={}, score=0.95944905, id=5)
// SearchResp.SearchResult(entity={}, score=0.8689616, id=1)
// SearchResp.SearchResult(entity={}, score=0.866088, id=7)

为 Search 临时设置一个时区
如果你的 Collection 包含 TIMESTAMPTZ 字段，你可以在一次操作中通过在 search 调用中设置 timezone 参数，临时覆盖 Database 或 Collection 的默认时区。这会控制在该次操作中 TIMESTAMPTZ 值的显示和比较方式。

以下示例展示了如何为 search 操作设置临时时区：

Python
Java
NodeJS
Go
cURL
// java

参数 timezone 的值必须是有效的 IANA 时区标识符，例如 Asia/Shanghai、America/Chicago 或 UTC。关于如何使用 TIMESTAMPTZ 字段的详细信息，请参见TIMESTAMPTZ 类型。

ANN Search 能力增强
Zilliz Cloud 提供的 AUTOINDEX 已经极大地降低了执行 ANN Search 的门槛，但在大规模召回的情况下依旧很难避免遇到类似返回与查询向量不相关的 Entity 等各种问题。按照缩小搜索范围、提升召回质量和提升召回结果多样性的思路，Milvus 提供了如下几种能力：

Filtered Search

ANN Search 支持在请求中携带过滤条件表达式，并在执行向量搜索前根据过滤条件表达式过滤出与之匹配的 Entity。通过这种方式，向量搜索的范围由整个 Collection 中的所有 Entity 缩小到与过滤条件表达式匹配的所有 Entity 了。

关于标量过滤和过滤条件表达式的更多内容，可查看Filtered Search和过滤表达式概览。

Range Search

Range Search 是通过指定相似度得分范围的方式提升 ANN Search 的召回质量。在执行 Range Search 时，Zilliz Cloud 会以 ANN Search 召回结果中与查询向量最相似的向量为圆心，以 Search 请求中指定的 radius 值为外圆半径，以 range_filter 为内圆半径画两个同心圆。所有相似度得分落在由这两个同心圆构成的圆环上的向量会被返回。

关于 Range Search 的更多内容，可查看 Range Search。

Grouping Search

如果召回结果中所有 Entity 在某个标量字段上的取值都相同时，召回结果可能并不能真实反映与查询向量相似的所有向量在向量空间中的分布情况。为了提升召回结果的多样性，可以考虑使用 Grouping Search。

关于 Grouping Search 的更多内容，可查看Grouping Search。

Hybrid Search

Zilliz Cloud 支持在创建 Collection 时设置多个向量字段，用于存放使用不同的 Embedding 模型生成的向量数据。在此基础上，您可以使用 Hybrid Search 功能混合不同向量字段的多路召回结果并对它们进行混合排序，尝试得到更为精准的召回结果。

关于 Hybrid Search 的更多内容，可查看Hybrid Search。

如需了解 Collection 中向量字段的数量限制，请参考使用限制。

Search Iterator

ANN Search 单次召回有最大数量限制。对于 topK 大于 16,384 的 ANN Search 请求，可以考虑使用 Search Iterator。

关于 Search Iterator 的更多内容，可查看Search Iterator。

Full-Text Search

Full-Text Search 是一项能在文本数据集中检索包含特定术语或短语的文档，然后根据相关性对结果进行排序的功能。该功能克服了语义搜索的局限性，语义搜索可能会忽略精确的术语，而全文搜索可确保你获得最准确且与上下文相关的结果。此外，它通过接受原始文本输入简化了向量搜索，能自动将你的文本数据转换为稀疏嵌入，无需手动生成向量嵌入。

关于 Full-Text Search 的更多内容，可查看Full Text Search。

Text Match

Zilliz Cloud 中的 Text Match 功能可基于特定术语实现精确的文档检索。此功能主要用于过滤式搜索，以满足特定条件，并且可以结合标量过滤来优化查询结果，从而在符合标量条件的向量中进行相似性搜索。

关于 Text Match 的更多内容，可查看Text Match。

使用 Partition Key

如果参与过滤的标量字段过多、过滤条件表达式过于复杂，都可能会对召回效率带来负面影响。Zilliz Cloud 提出了 Partition Key 这个概念。通过将 Collection 中某一标量字段指定为 Partition Key，并在 Search 请求中使用仅包含 Partition Key 的过滤条件表达式，可以快速将搜索范围缩小到指定 Partition Key 值对应的若干 Partition。

关于 Partition Key 的更多内容，可查看使用 Partition Key。

使用 mmap

关于 mmap 设置的相关内容，可以查看使用 mmap。

Filtered Search
近似最近邻（ANN） Search 可以根据指定的非结构化数据（向量）找到与之相似的一批非结构化数据（向量），但是无法做到精确匹配。对于简单的精确匹配需求，可以使用过滤条件表达式基于部分标量字段进行文本过滤。本节将介绍如何在 ANN Search 中使用过滤条件表达式及相关注意事项。

概述
在 Zilliz Cloud 中，Filtered Search 分为两种类型，即标准 Filtered Search 和 迭代 Filtered Search。这两种类型的区别在于何时进行标量过滤。

标准 Filtered Search
如果 Collection 中既存放了非结构化数据的向量表示，也存放了这些非结构化数据的各类属性，您就可以使用这些属性字段构建过滤条件表达式，并将构建好的表达式包含在 ANN Search 请求中。Zilliz Cloud 在收到 Search 请求后，如果发现请求中携带了过滤条件表达式，就会按照过滤条件表达式中的条件找到所有与之匹配的 Entity，然后再根据请求中携带的查询向量在与过滤条件匹配的 Entity 中查找 topK 个与查询向量相似的 Entity。

MiVRw4H1KhPxAVb6L5icIczTnCc

如上图所示，Search 请求中携带了一个过滤条件表达式 chunk like "%red%"，表示需要在进行 ANN Search 前先找出 chunk 字段中包含 red 这个单词的所有 Entity。Zilliz Cloud 在收到这个 Search 请求时，会执行如下步骤：

过滤出符合过滤条件表达式的所有 Entity。

根据查询向量在匹配过滤条件表达式的所有 Entity 中进行 ANN Search。

返回 topK 个 Entity。

迭代 Filtered Search
标准 Filtered Search 的标量过滤过程有效地将搜索限定在一个较小的范围。然而，过于复杂的过滤表达式可能会导致非常高的搜索延迟。在这种情况下，迭代 Filtered Search 可以作为一种替代方案，有助于减少标量过滤的工作量。

XSNPwbb6uhqnlqb9EXwcOybhnae

如上图所示，一个迭代 Filtered Search 请求会将搜索过程分成多次迭代进行。在每次迭代中，会先进行向量搜索，然后再进行标量过滤，并去除掉本次迭代中不满足标量过滤条件的结果。当 topK 满足后，返回最终结果。

这种方式显著地减少了标量过滤需要处理的 Entity 数量，使得其尤其适用于处理过滤条件较为复杂的场景。

需要注意的是，迭代器每次只处理一个 Entity，并以串行的方式进行迭代。使用迭代 Filtered Search 可能会拉长请求处理时间或在需要处理的 Entity 较大时出现潜在的性能问题。

操作示例
本节将结合具体的示例介绍如何进行 Filtered Search。在如下示例中，假设 Collection 已经存放了如下 10 条Entity。每个 Entity 都有 id, vector, color 和 likes 这几个字段。

[
{"id": 0, "vector": [0.3580376395471989, -0.6023495712049978, 0.18414012509913835, -0.26286205330961354, 0.9029438446296592], "color": "pink_8682", "likes": 165},
{"id": 1, "vector": [0.19886812562848388, 0.06023560599112088, 0.6976963061752597, 0.2614474506242501, 0.838729485096104], "color": "red_7025", "likes": 25},
{"id": 2, "vector": [0.43742130801983836, -0.5597502546264526, 0.6457887650909682, 0.7894058910881185, 0.20785793220625592], "color": "orange_6781", "likes": 764},
{"id": 3, "vector": [0.3172005263489739, 0.9719044792798428, -0.36981146090600725, -0.4860894583077995, 0.95791889146345], "color": "pink_9298", "likes": 234},
{"id": 4, "vector": [0.4452349528804562, -0.8757026943054742, 0.8220779437047674, 0.46406290649483184, 0.30337481143159106], "color": "red_4794", "likes": 122},
{"id": 5, "vector": [0.985825131989184, -0.8144651566660419, 0.6299267002202009, 0.1206906911183383, -0.1446277761879955], "color": "yellow_4222", "likes": 12},
{"id": 6, "vector": [0.8371977790571115, -0.015764369584852833, -0.31062937026679327, -0.562666951622192, -0.8984947637863987], "color": "red_9392", "likes": 58},
{"id": 7, "vector": [-0.33445148015177995, -0.2567135004164067, 0.8987539745369246, 0.9402995886420709, 0.5378064918413052], "color": "grey_8510", "likes": 775},
{"id": 8, "vector": [0.39524717779832685, 0.4000257286739164, -0.5890507376891594, -0.8650502298996872, -0.6140360785406336], "color": "white_9381", "likes": 876},
{"id": 9, "vector": [0.5718280481994695, 0.24070317428066512, -0.3737913482606834, -0.06726932177492717, -0.6980531615588608], "color": "purple_4976", "likes": 765}
]


📘说明
如果查询向量已经在目标 Collection 中存在，可以考虑使用 ids 参数，从而让 Milvus 在搜索前从 Collection 中自动获取查询向量。更多内容，可以阅读 Primary Key Search。

使用标准 Filtered Search
当使用如下示例代码在上述 Entity 中进行搜索时，我们需要在 Search 请求中添加过滤条件。为了方便检查搜索结果是否满足过滤条件，我们还在 Search 请求中指定了 Output Fields。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp

MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
.uri("YOUR_CLUSTER_ENDPOINT")
.token("YOUR_CLUSTER_TOKEN")
.build());

FloatVec queryVector = new FloatVec(new float[]{0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f});
SearchReq searchReq = SearchReq.builder()
.collectionName("my_collection")
.data(Collections.singletonList(queryVector))
.topK(5)
.filter("color like \"red%\" and likes > 50")
.outputFields(Arrays.asList("color", "likes"))
.build();

SearchResp searchResp = client.search(searchReq);

List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
for (List<SearchResp.SearchResult> results : searchResults) {
System.out.println("TopK results:");
for (SearchResp.SearchResult result : results) {
System.out.println(result);
}
}

// Output
// TopK results:
// SearchResp.SearchResult(entity={color=red_4794, likes=122}, score=0.5975797, id=4)
// SearchResp.SearchResult(entity={color=red_9392, likes=58}, score=-0.24996188, id=6)

在上述代码示例中使用的过滤条件表达式 color like "red%" and likes > 50 中包含了使用 and 操作符连接的两个过滤条件：一个是 color 字段的值以 red 开头，另一个是 likes 字段的值大于 50。在示例数据中，符合此条件的数据只有两条，因此在满足 topK 小于等于 3 的情况下，这两条数据会全部返回。

[
{
"id": 4,
"distance": 0.3345786594834839,
"entity": {
"vector": [0.4452349528804562, -0.8757026943054742, 0.8220779437047674, 0.46406290649483184, 0.30337481143159106],
"color": "red_4794",
"likes": 122
}
},
{
"id": 6,
"distance": 0.6638239834383389，
"entity": {
"vector": [0.8371977790571115, -0.015764369584852833, -0.31062937026679327, -0.562666951622192, -0.8984947637863987],
"color": "red_9392",
"likes": 58
}
},
]


关于在过滤条件表达式中可以使用的操作符，可以参考过滤表达式概览。

使用迭代 Filtered Search
要使用迭代 Filtered Search 进行过滤搜索，您可以执行以下操作：

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;

MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
.uri("YOUR_CLUSTER_ENDPOINT")
.token("YOUR_CLUSTER_TOKEN")
.build());

FloatVec queryVector = new FloatVec(new float[]{0.3580376395471989f, -0.6023495712049978f, 0.18414012509913835f, -0.26286205330961354f, 0.9029438446296592f});
SearchReq searchReq = SearchReq.builder()
.collectionName("my_collection")
.data(Collections.singletonList(queryVector))
.topK(5)
Zilliz Cloud 除了支持 ANN Search 外，还提供基于标量的过滤查询功能。本节将介绍如何使用 Query、Get 和 QueryIterator 进行标量查询以及进行标量查询时的注意事项。

概述
Collection 中可以存储多种类型的标量字段。您可以让 Milvus 基于一个或多个标量字段进行过滤查询，找出符合指定条件的部分或所有 Entity。Zilliz Cloud 提供了三种过滤查询的方法，分别为 Query、Get 和 QueryIterator。下表对这三种过滤查询方法进行了比较。

Get

Query

QueryIterator

适用场景

要求根据主键值查询，返回指定 Entity。

要求根据自定义条件查询，且返回所有或指定数量的符合条件的 Entity。

要求根据自定义条件查询，且分页返回所有符合条件的 Entity。

过滤方法

基于主键字段过滤。

基于过滤条件表达式过滤。

基于过滤条件表达式过滤。

必选参数

Collection 名称

主键值

Collection 名称

过滤条件表达式

Collection 名称

过滤条件表达式

单页返回 Entity 数量

可选参数

Partition 名称

返回 Entity 携带字段名称

Partition 名称

返回 Entity 数量

返回 Entity 携带字段名称

Partition 名称

返回 Entity 总数量

返回 Entity 携带字段名称

返回结果

返回 Collection 或指定 Partition 中符合指定主键值的 Entity。

返回 Collection 或指定 Partition 中所有或指定数量的符合指定过滤条件的 Entity。

分页返回 Collection 或指定 Partition 中所有符合指定过滤条件的 Entity。

关于过滤条件表达式的更多细节，可参考过滤表达式概览。

使用 Get
当您需要根据 Entity 主键从 Collection 或 Partition 中查询 Entity 时，可以使用 Get 方法。如下代码示例中假设 Collection 有 id、vector 和 color 三个字段。

[
{"id": 0, "vector": [0.3580376395471989, -0.6023495712049978, 0.18414012509913835, -0.26286205330961354, 0.9029438446296592], "color": "pink_8682"},
{"id": 1, "vector": [0.19886812562848388, 0.06023560599112088, 0.6976963061752597, 0.2614474506242501, 0.838729485096104], "color": "red_7025"},
{"id": 2, "vector": [0.43742130801983836, -0.5597502546264526, 0.6457887650909682, 0.7894058910881185, 0.20785793220625592], "color": "orange_6781"},
{"id": 3, "vector": [0.3172005263489739, 0.9719044792798428, -0.36981146090600725, -0.4860894583077995, 0.95791889146345], "color": "pink_9298"},
{"id": 4, "vector": [0.4452349528804562, -0.8757026943054742, 0.8220779437047674, 0.46406290649483184, 0.30337481143159106], "color": "red_4794"},
{"id": 5, "vector": [0.985825131989184, -0.8144651566660419, 0.6299267002202009, 0.1206906911183383, -0.1446277761879955], "color": "yellow_4222"},
{"id": 6, "vector": [0.8371977790571115, -0.015764369584852833, -0.31062937026679327, -0.562666951622192, -0.8984947637863987], "color": "red_9392"},
{"id": 7, "vector": [-0.33445148015177995, -0.2567135004164067, 0.8987539745369246, 0.9402995886420709, 0.5378064918413052], "color": "grey_8510"},
{"id": 8, "vector": [0.39524717779832685, 0.4000257286739164, -0.5890507376891594, -0.8650502298996872, -0.6140360785406336], "color": "white_9381"},
{"id": 9, "vector": [0.5718280481994695, 0.24070317428066512, -0.3737913482606834, -0.06726932177492717, -0.6980531615588608], "color": "purple_4976"},
]


您可以参考如下代码示例获取主键值为 0、1、2 的三个 Entity。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.GetReq
import io.milvus.v2.service.vector.request.GetResp
import io.milvus.v2.service.vector.response.QueryResp;
import java.util.*;

MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
.uri("YOUR_CLUSTER_ENDPOINT")
.token("YOUR_CLUSTER_TOKEN")
.build());

GetReq getReq = GetReq.builder()
.collectionName("my_collection")
.ids(Arrays.asList(0, 1, 2))
.outputFields(Arrays.asList("vector", "color"))
.build();

GetResp getResp = client.get(getReq);

List<QueryResp.QueryResult> results = getResp.getGetResults();
for (QueryResp.QueryResult result : results) {
System.out.println(result.getEntity());
}

// Output
// {color=pink_8682, vector=[0.35803765, -0.6023496, 0.18414013, -0.26286206, 0.90294385], id=0}
// {color=red_7025, vector=[0.19886813, 0.060235605, 0.6976963, 0.26144746, 0.8387295], id=1}
// {color=orange_6781, vector=[0.43742132, -0.55975026, 0.6457888, 0.7894059, 0.20785794], id=2}

使用 Query
当您需要根据自定义条件查询，且返回所有或指定数量的符合条件的 Entity 时，可以使用 Query 方法。如下代码示例中假设 Collection 有 id、vector 和 color 三个字段。要求返回三个 color 以 red 开头的 Entity。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.service.vector.request.QueryReq
import io.milvus.v2.service.vector.request.QueryResp

QueryReq queryReq = QueryReq.builder()
.collectionName("my_collection")
.filter("color like \"red%\"")
.outputFields(Arrays.asList("vector", "color"))
.limit(3)
.build();

QueryResp queryResp = client.query(queryReq);

List<QueryResp.QueryResult> results = queryResp.getQueryResults();
for (QueryResp.QueryResult result : results) {
System.out.println(result.getEntity());
}

// Output
// {color=red_7025, vector=[0.19886813, 0.060235605, 0.6976963, 0.26144746, 0.8387295], id=1}
// {color=red_4794, vector=[0.44523495, -0.8757027, 0.82207793, 0.4640629, 0.3033748], id=4}
// {color=red_9392, vector=[0.8371978, -0.015764369, -0.31062937, -0.56266695, -0.8984948], id=6}

使用 QueryIterator
当您需要根据自定义条件查询，且分页返回所有符合条件的 Entity，可以使用 QueryIterator 创建一个迭代器。然后使用迭代器的 next() 方法循环遍历所有符合条件的 Entity。如下代码示例中假设 Collection 有 id、vector 和 color 三个字段。要求返回所有 color 以 red 开头的 Entity。

Python
Java
Go
NodeJS
cURL
import io.milvus.orm.iterator.QueryIterator;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.service.vector.request.QueryIteratorReq;

QueryIteratorReq req = QueryIteratorReq.builder()
.collectionName("my_collection")
.expr("color like \"red%\"")
.batchSize(10L)
.outputFields(Collections.singletonList("color"))
.build();
QueryIterator queryIterator = client.queryIterator(req);

while (true) {
List<QueryResultsWrapper.RowRecord> res = queryIterator.next();
if (res.isEmpty()) {
queryIterator.close();
break;
}

    for (QueryResultsWrapper.RowRecord record : res) {
        System.out.println(record);
    }
}

// Output
// [color:red_7025, id:1]
// [color:red_4794, id:4]
// [color:red_9392, id:6]

在 Partition 中过滤查询
除了在 Collection 中进行过滤查询外，还可以在指定的一个或多个 Partition 中进行过滤查询，只需要在上面的 Get、Query 和 QueryIterator 方法中增加 Partition 名称即可。如下示例代码中假设 Collection 中存在一个名为 PartitionA 的 Partition。

Python
Java
Go
NodeJS
cURL
GetReq getReq = GetReq.builder()
.collectionName("my_collection")
.partitionName("partitionA")
.ids(Arrays.asList(10, 11, 12))
.outputFields(Collections.singletonList("color"))
.build();

GetResp getResp = client.get(getReq);

QueryReq queryReq = QueryReq.builder()
.collectionName("my_collection")
.partitionNames(Collections.singletonList("partitionA"))
.filter("color like \"red%\"")
.outputFields(Collections.singletonList("color"))
.limit(3)
.build();

QueryResp getResp = client.query(queryReq);

QueryIteratorReq req = QueryIteratorReq.builder()
.collectionName("my_collection")
.partitionNames(Collections.singletonList("partitionA"))
.expr("color like \"red%\"")
.batchSize(50L)
.outputFields(Collections.singletonList("color"))
.consistencyLevel(ConsistencyLevel.BOUNDED)
.build();
QueryIterator queryIterator = client.queryIterator(req);

使用 Query 进行随机取样
若要从数据集中提取具有代表性的数据子集用于数据探索或开发测试，请使用 RANDOM_SAMPLE(sampling_factor) 表达式，其中 sampling_factor 是一个介于 0 和 1 之间的浮点数，表示要采样的数据百分比。

📘说明
关于随机采样的详细使用方法、高级示例和最佳实践，可参考随机采样。

Python
Java
Go
NodeJS
cURL
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.GetReq
import io.milvus.v2.service.vector.request.GetResp
import io.milvus.v2.service.vector.request.QueryReq
import io.milvus.v2.service.vector.request.QueryResp
import java.util.*;

MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
.uri("YOUR_CLUSTER_ENDPOINT")
.token("YOUR_CLUSTER_TOKEN")
.build());

QueryReq queryReq = QueryReq.builder()
.collectionName("my_collection")
.filter("RANDOM_SAMPLE(0.01)")
.outputFields(Arrays.asList("vector", "color"))
.build();

QueryResp getResp = client.query(queryReq);
for (QueryResp.QueryResult result : getResp.getQueryResults()) {
System.out.println(result.getEntity());
}

queryReq = QueryReq.builder()
.collectionName("my_collection")
.filter("color like \"red%\" AND RANDOM_SAMPLE(0.005)")
.outputFields(Arrays.asList("vector", "color"))
.limit(10)
.build();

getResp = client.query(queryReq);
for (QueryResp.QueryResult result : getResp.getQueryResults()) {
System.out.println(result.getEntity());
}

为 Query 临时设置一个时区
如果你的 Collection 包含 TIMESTAMPTZ 字段，你可以在一次操作中通过在 query 调用中设置 timezone 参数，临时覆盖 Database 或 Collection 的默认时区。这会控制在该次操作中 TIMESTAMPTZ 值的显示和比较方式。

以下示例展示了如何为 query 操作设置临时时区：

Python
Java
NodeJS
Go
cURL
// java

参数 timezone 的值必须是有效的 IANA 时区标识符，例如 Asia/Shanghai、America/Chicago 或 UTC。关于如何使用 TIMESTAMPTZ 字段的详细信息，请参见TIMESTAMPTZ 类型。