package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService extends RedisKeyUtil {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // Caffeine核心接口：Cache, LoadingCache, AsyncLoadingCache

    // 帖子列表的缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数的缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    // 可参考文章 https://www.jianshu.com/p/15d0a9ce37dd

    @PostConstruct
    public void init(){
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length != 2){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 二级缓存：Redis -> mysql
                        String redisKey = RedisKeyUtil.getHotPostListKey(offset,limit);
                        // 如果redis中不存在就去数据库里查，查到了就更新到redis中，然后返回作为本地缓存的同步
                        if (!redisTemplate.hasKey(redisKey)){
                            logger.debug("redis中没有缓存");
                            logger.debug("load post list from DB.");
                            List<DiscussPost> result = discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                            for(DiscussPost post : result){
                                redisTemplate.opsForZSet().add(redisKey,post,post.getScore());
                            }
                            redisTemplate.expire(redisKey,7 * 60 ,TimeUnit.SECONDS);
                            return result;
                        } else {
                            logger.debug("redis中有缓存");
                            // 要查看redis数据是否过期
                            System.out.println(redisTemplate.getExpire(redisKey));
                            boolean isExpired = redisTemplate.getExpire(redisKey) < 0 ? true : false;
                            if (isExpired){
                                // 如果过期的话，就删去key，就去数据库里查
                                logger.debug("redis expired!");
                                redisTemplate.delete(redisKey);
                                logger.debug("load post list from DB.");
                                List<DiscussPost> result = discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                                for(DiscussPost post : result){
                                    redisTemplate.opsForZSet().add(redisKey,post,post.getScore());
                                }
                                redisTemplate.expire(redisKey,7*60 ,TimeUnit.SECONDS);
                                return result;
                            } else {
                                Set<DiscussPost> posts = redisTemplate.opsForZSet().reverseRange(redisKey,offset,limit);
                                logger.debug("redis len: " + redisTemplate.opsForZSet().zCard(redisKey));
                                logger.debug("load post list from redis.");
                                List<DiscussPost> list = new ArrayList<>();
                                for(DiscussPost post : posts){
                                    list.add(post);
                                }
                                return list;
                            }
                        }
                    }
                });

        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    //userId是外键
    public List<DiscussPost> findDiscussPosts(int userId, int offset,int limit, int orderMode){
        // 只有当查询首页的热门帖子时才使用缓存
//        if (userId == 0 && orderMode == 1){
//            // 如果没有查到会调用其load方法同步数据，这是阻塞的
//            return postListCache.get(offset + ":" + limit);
//        }
//        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId,offset,limit,orderMode);
    }

    public int findDiscussPostRows(int userId){
//        if (userId == 0){
//            return postRowsCache.get(0);
//        }
//        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post){
        if (post == null){
            throw new IllegalArgumentException("参数不能为空");
        }

        // 转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostByid(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    public int updateType(int id,int type){
        return discussPostMapper.updateType(id,type);
    }

    public int updateStatus(int id,int status){
        return discussPostMapper.updateStatus(id,status);
    }

    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id,score);
    }
}
