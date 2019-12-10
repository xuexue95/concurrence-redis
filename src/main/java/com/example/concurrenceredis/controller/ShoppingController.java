package com.example.concurrenceredis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class ShoppingController {

    @Autowired
    private RedisTemplate <String,String> redisTemplate;

    //记录实际卖出的商品数量
    private AtomicInteger successNum = new AtomicInteger(0);

    @RequestMapping("/init")
    public void productInit(){
        redisTemplate.opsForHash().put("product","小米10","20");
        System.out.println(redisTemplate.opsForHash().get("product","小米10"));
    }

    @RequestMapping("/rushShop")
    public String rushShop(String productName,int userId){
        Double storeValue =  Double.parseDouble((String) redisTemplate.opsForHash().get("product",productName));
        System.out.println(storeValue);
        if (storeValue == null || storeValue == 0){
            System.out.println(redisTemplate.opsForList().range(productName  + ":sale", 0, -1));
            return "售尽";
        } else {
            // 库存减一
//            redisTemplate.setEnableTransactionSupport(true);
//            redisTemplate.multi();
            redisTemplate.opsForHash().increment("product",productName,-1);
            // 存放进一个list 中
            redisTemplate.opsForList().rightPush(productName +":sale", String.valueOf(userId));
            System.out.println(redisTemplate.opsForList().range(productName  + ":sale", 0, -1));
            successNum.incrementAndGet();
//            redisTemplate.exec();
            return ("成功");
        }
    }
}
