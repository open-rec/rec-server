package com.openrec.proto;


import lombok.Data;

@Data
public class HealthRes extends JsonRes{

    public HealthRes(){
        this.code = ProtoCode.SUCCESS;
        this.status = true;
        this.msg = "health check";
    }
}
