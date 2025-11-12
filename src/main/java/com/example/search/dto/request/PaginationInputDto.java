package com.example.search.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaginationInputDto {
    private Integer page;
    private Integer size;
}


