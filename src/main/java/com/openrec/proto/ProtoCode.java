package com.openrec.proto;

import org.springframework.http.HttpStatus;

public interface ProtoCode {

    int SUCCESS = HttpStatus.OK.value();
    int NOT_FOUND = HttpStatus.NOT_FOUND.value();
    int ERROR = HttpStatus.INTERNAL_SERVER_ERROR.value();
}
