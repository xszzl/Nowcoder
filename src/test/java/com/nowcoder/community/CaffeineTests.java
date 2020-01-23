package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CaffeineTests {

    @Autowired
    private DiscussPostService postService;

    @Autowired
    private RedisTemplate redisTemplate;


    @Test
    public void initDataForTest(){
        for (int i = 0; i < 300000; i++) {
            System.out.println(i);
            DiscussPost post = new DiscussPost();
            post.setUserId(111);
            post.setTitle("互联网求职暖春计划");
            post.setContent("今年的就业形式，确实不容乐观，过了个年，仿佛跳水一样");
            post.setCreateTime(new Date());
            post.setScore(Math.random() * 2000);
            postService.addDiscussPost(post);
        }
    }

    @Test
    public void testCache(){
        String redisKey = "post:0:10";
        redisTemplate.delete(redisKey);

        // 从db
        postService.findDiscussPosts(0,0,10,1);
        System.out.println("===========================");

        System.out.println("数据有"+redisTemplate.opsForZSet().zCard(redisKey)+"条");
        // 从本地缓存
        System.out.println("从一级缓存!");
        postService.findDiscussPosts(0,0,10,1);
        System.out.println("===========================");

        try {
            Thread.sleep(12 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 从redis
        System.out.println("从redis！");
        postService.findDiscussPosts(0,0,10,1);
        System.out.println("===========================");
        try {
            Thread.sleep(12 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(redisTemplate.hasKey(redisKey));
        // 从db
        System.out.println("从db");
        postService.findDiscussPosts(0,0,10,1);
        System.out.println("===========================");
    }

    @Test
    public void testRedisZset(){
        DiscussPost post = new DiscussPost();
        post.setUserId(111);
        post.setTitle("互联网求职暖春计划");
        post.setContent("今年的就业形式，确实不容乐观，过了个年，仿佛跳水一样");
        post.setCreateTime(new Date());
        post.setScore(Math.random() * 2000);
        String redisKey = "post:10:20";
        String expiredKey = redisKey + ":expired";

        redisTemplate.delete(redisKey);
        System.out.println(redisTemplate.hasKey(redisKey));

        redisTemplate.opsForValue().set(expiredKey,1);
        redisTemplate.opsForZSet().add(redisKey,post,post.getScore());

        // 设置过期时间
        redisTemplate.expire(expiredKey,5, TimeUnit.SECONDS);
        redisTemplate.expire(redisKey,5,TimeUnit.SECONDS);

        System.out.println(redisTemplate.getExpire(expiredKey));
        System.out.println(redisTemplate.getExpire(redisKey));

        try {
            Thread.sleep(7*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(redisTemplate.getExpire(expiredKey));
        System.out.println(redisTemplate.getExpire(redisKey));
    }
}
