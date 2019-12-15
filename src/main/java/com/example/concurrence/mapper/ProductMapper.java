package com.example.concurrence.mapper;

import com.example.concurrence.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM rush_product")
    List<Product> getAll();
}
