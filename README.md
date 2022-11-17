# rec-server

## contrib

User custom contribution module. Only support operation rule custom now.
After release the rec-server could load the plugin and exec the custom operation rule by config `operationName`

## graph

Async DAG tool package.

![graph](doc/graph.jpeg "graph")

## proto

Common protocols with rec-client. Include recommend, query, push and operate.

## server

The real recommend online server.

### function point

Function Point | Available
------------- | -------------
user&item trigger | ✅
blacklist  | ✅
exposure filter | ✅
i2i  | ✅
embedding | ✅
hot | ✅
new  | ✅
rank | ❎
custom operation | ✅
debug info | ✅