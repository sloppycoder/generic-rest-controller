package org.vino9.demo.genericrestcontroller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;

public class RestControllerUtils {

    static public int getParamAsInt(Map<String, String> params, String key, int defaultValue) {
        return params.containsKey(key) ? Integer.valueOf(params.get(key)).intValue() : defaultValue;
    }

    static public long getParamAsLong(Map<String, String> params, String key, long defaultValue) {
        return params.containsKey(key) ? Long.valueOf(params.get(key)).longValue() : defaultValue;
    }

    static public Pageable getPageableFromParams(Map<String, String> params, int defaultPageSize) {
        int page = getParamAsInt(params, "page", 0);
        int perPage = getParamAsInt(params, "per_page", defaultPageSize);

        return PageRequest.of(page, perPage);
    }

    static public Map<String, Object> paginationResult(Page<?> currPage, String url) {
        int pageNo = currPage.getNumber();
        int perPage = currPage.getSize();

        HashMap<String, Object> meta = new HashMap<>(Map.of(
                "page", pageNo,
                "per_page", perPage,
                "curr", getUrlForPageable(currPage.getPageable(), url)
        ));

        if (currPage.hasPrevious()) {
            meta.put("prev", getUrlForPageable(currPage.previousPageable(), url));
        }

        if (currPage.hasNext()) {
            meta.put("next", getUrlForPageable(currPage.nextPageable(), url));
        }

        return Map.of(
                "_meta_", meta,
                "data", currPage.getContent()
        );
    }

    static public String getUrlForPageable(Pageable pageable, String url) {
        return String.format("%s?page=%d&per_page=%d", url, pageable.getPageNumber(), pageable.getPageSize());
    }
}