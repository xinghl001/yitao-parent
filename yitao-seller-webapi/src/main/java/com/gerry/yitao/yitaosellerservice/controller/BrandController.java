package com.gerry.yitao.yitaosellerservice.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.gerry.yitao.domain.Brand;
import com.gerry.yitao.entity.PageResult;
import com.gerry.yitao.service.BrandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @ProjectName: yitao-parent
 * @Auther: GERRY
 * @Date: 2019/4/16 21:36
 * @Description:
 */
@RestController
@RequestMapping("api/item/brand")
public class BrandController {

    @Reference
    private BrandService brandService;

    @GetMapping("page")
    public ResponseEntity<PageResult<Brand>> queryBrandByPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "8") Integer rows,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
            @RequestParam(value = "key", required = false) String key
    ) {

        return ResponseEntity.ok(brandService.queryBrandByPageAndSort(page, rows,sortBy, desc, key));
    }
}
