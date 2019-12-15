# SpringBoot + redis +mysql 抗高并发



### 一、模拟场景

商城秒杀活动，用户对参加活动商品下单

使用 Jmeter 模拟高并发场景

#### 思路：

1. 从 mysql 获取活动商品 存入 redis

2. 当收到抢购请求时：

	（1）判断库存是否大于零 （并发时会穿透判断）

	（2）判断用户是否参加过活动 （利用 set 的特性判断）

	（3）是否有库存，减库存 （库存自增-1，判断减库存后库存是否小于零）

	（4）生成订单

3. 将 redis 订单列表间断性存入 mysql

4. 使用 Jmeter 测试

### 二、Mysql 表结构

#### 1. 库存表

![商品库存表](https://github.com/xuexue95/concurrence-redis/blob/master/images/商品库存表.png)

#### 2. 订单表

![订单表](https://github.com/xuexue95/concurrence-redis/blob/master/images/订单表.png)

### 三、项目依赖

* Spring Web
* MySQL Driver
* MyBatis Framework
* Spring Data Reactive Redis



### 四、项目目录

![项目目录](https://github.com/xuexue95/concurrence-redis/blob/master/images/项目目录.png)



### 五、数据库配置

application.properties

```
# mysql
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/java?serverTimezone=UTC&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# redis
spring.redis.host=localhost
spring.redis.port=6379
```



### 六、entity

#### 1. Order

```java
package com.example.concurrence.entity;

public class Order {
    private int id;
    private String productName;
    private int userId;
    
    // getter/setter...
}
```



#### 2. Product

```java
package com.example.concurrence.entity;

public class Product {
    private int id;
    private String productName;
    private int stock;
    
    // getter/setter...
}
```



### 七、mapper

#### 1. OrderMapper

```java
package com.example.concurrence.mapper;

import com.example.concurrence.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    
    @Insert("Insert into rush_order (productName,userId) values(#{productName},#{userId})")
    void insertOrder(Order order);
}
```



#### 2. ProductMapper

```java
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
```



### 八、service

ProductService

```java
package com.example.concurrence.service;

import com.example.concurrence.entity.Order;
import com.example.concurrence.entity.Product;
import com.example.concurrence.mapper.OrderMapper;
import com.example.concurrence.mapper.ProductMapper;
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

    /**
     * 获取缓存订单,存入mysql
     */
    public void setOrderToMysql() {
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
```



### 九、controller

ProductController

```java
package com.example.concurrence.controller;

import com.example.concurrence.entity.Product;
import com.example.concurrence.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    @Autowired
    ProductService productService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;


    /**
     * 初始化商品库存接口
     * 从 mysql 获取商品库存,存入 redis
     * @return String
     */
    @RequestMapping("/productInit")
    public String productInit(){
        List<Product> products =  productService.getProductFromMysql();

        for (Product product : products) {
            redisTemplate.opsForValue().set(product.getProductName(), String.valueOf(product.getStock()));
        }
        return "init success";
    }


    /**
     * 抢购接口
     * @param productName 商品名称
     * @param userId 用户id
     * @return String
     */
    @RequestMapping("/rush")
    public String rush(String productName,int userId){
        System.out.println("商品名称:"+ productName +"用户ID:" +userId);

        // 模拟高并发
//        for(int i=0;i<500;i++){
//            int finalI = i;
//            Thread thread=new Thread(() -> {
//                String res =  productService.createOrder(productName, finalI);
//                System.out.println(res);
//            });
//            thread.start();
//        }
        String res =  productService.createOrder(productName, userId);
        return res;
    }
}
```



### 十、主入口

ConcurrenceApplication

```
package com.example.concurrence;

import com.example.concurrence.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class ConcurrenceApplication {

	@Autowired
	private ProductService productService;
	public static void main(String[] args) {
		SpringApplication.run(ConcurrenceApplication.class, args);

	}
	
	// 计划任务 每隔 0.5秒 获取 redis 订单列表存入 mysql
	@Scheduled(fixedDelay = 500)
	public void autoRun() {
		productService.setOrderToMysql();
	}
}
```

