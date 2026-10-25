package com.example.demo.service.cursor;

import com.example.demo.cursor.SortDirection;
import com.example.demo.dto.response.CursorPageResponse;

public interface CursorPageService<Context, Data> {

    CursorPageResponse<Data> page(String rawCursor,
                                  SortDirection sortDirection,
                                  String query,
                                  Context context);
}