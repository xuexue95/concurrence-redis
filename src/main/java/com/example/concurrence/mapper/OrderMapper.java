package com.example.concurrence.mapper;

import com.example.concurrence.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    @Insert("Insert into rush_order (productName,userId) values(#{productName},#{userId})")
    void insertOrder(Order order);
}
