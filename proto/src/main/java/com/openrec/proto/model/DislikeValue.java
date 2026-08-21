package com.openrec.proto.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import lombok.Data;

/** Structured value carried by a dislike event. All fields are optional, but one must be present. */
@Data
public class DislikeValue implements Serializable {
    private String id;
    private String category;
    private List<String> tags = Collections.emptyList();
}
