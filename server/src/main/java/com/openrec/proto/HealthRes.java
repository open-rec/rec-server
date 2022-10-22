package com.openrec.proto;


import lombok.Data;
import com.openrec.proto.JsonRes;

@Data
public class HealthRes extends JsonRes{

    public HealthRes(){
        this.code = ProtoCode.SUCCESS;
        this.status = true;
        this.msg = "health check";
    }
}
