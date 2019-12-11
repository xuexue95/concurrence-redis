package com.example.concurrence.service;
import	java.awt.Desktop.Action;

import com.example.concurrence.entity.Order;
import com.example.concurrence.entity.Product;
import com.example.concurrence.mapper.OrderMapper;
import com.example.concurrence.mapper.ProductMapper;
import com.sun.org.apache.xpath.internal.operations.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;


    /**
     * 从数据库获取商品以及库存
     * @return 商品列表[{productNum:"test1",sock:100}]
     */
    public List<Product> getProductFromMysql(){
        return productMapper.getAll() ;
    }


    /**
     * 创建订单
     * @param productName 商品名称/id
     * @param userId 用户id
     * @return String
     */
    public String createOrder(String productName, int userId){
        int num = Integer.parseInt(redisTemplate.opsForValue().get(productName));
        if (num <= 0 ){
            return "已售尽";
        }

        // 判断用户是否参加过活动 集合添加成功返回1,失败返回0
         int userNum = Integer.parseInt(String.valueOf(redisTemplate.opsForSet().add(productName+"_userId",String.valueOf(userId))));

         if (userNum == 0){
             return "请勿重复抢购";
         }

        // 是否有库存,减库存
        int sockNow = Integer.parseInt(String.valueOf(redisTemplate.opsForValue().increment(productName, -1)));

        if(sockNow < 0){
            redisTemplate.opsForSet().remove(productName+"_userId",String.valueOf(userId));
            return "已售尽";
        }

        // 生成订单
        redisTemplate.opsForList().rightPush(productName+"_order", String.valueOf(userId));
        System.out.println(redisTemplate.opsForList().range(productName+"_order" , 0, -1));
        return "购买成功";
    }


    public void setOrderToMysql() throws InterruptedException {
        List<Product> products = productMapper.getAll() ;
        for (Product product : products) {
            // 获取 redis 订单列表长度
            int orderNum = Integer.parseInt(String.valueOf(redisTemplate.opsForList().size(product.getProductName()+"_order")));
            if(orderNum > 0 ){
                for (int j = 0; j < orderNum ;j++){
                    int userId = Integer.parseInt(redisTemplate.opsForList().rightPop(product.getProductName()+"_order"));
                    Order order = new Order();
                    order.setProductName(product.getProductName());
                    order.setUserId(userId);
                    orderMapper.insertOrder(order);
                }
            }
        }
    }
}
