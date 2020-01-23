package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Test
    public void testInsert(){
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
    }

//    @Test
//    public void testInsertList(){
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(101,0,100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(102,0,100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(103,0,100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(111,0,100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(112,0,100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(131,0,100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(132,0,100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(133,0,100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(134,0,100));
//    }

    @Test
    public void testUpdate(){
        DiscussPost post = discussPostMapper.selectDiscussPostById(231);
        post.setContent("我是新人，使劲灌水.");
        discussPostRepository.save(post);
    }

    @Test
    public void testDelete(){
        discussPostRepository.deleteById(231);
    }

    @Test
    public void testSearchByRepository() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 构造多字段查询
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                // 设置排序字段为type,score,createTime 倒序
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // 设置分页 page表示当前页
                .withPageable(PageRequest.of(0,10))
                // 设置高亮的前标签和后标签
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        // 将构造好的查询条件传入，得到全部结果放入Page中
        // 底层实现 使用elasticsearchTemplate.queryForPage(searchQuery, class, searchResultMapper)
        // 底层获取到了高亮显示的值，但是没有返回
        Page<DiscussPost> page = discussPostRepository.search(searchQuery);
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page){
            System.out.println(post);
        }

    }

    @Test
    public void testSearchByTemplate(){
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                // 构造多字段查询
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                // 设置排序字段为type,score,createTime 倒序
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // 设置分页 page表示当前页
                .withPageable(PageRequest.of(0,10))
                // 设置高亮的前标签和后标签
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // elasticsearchTemplate得到结果交给SearchResultMapper处理之后再返回
        Page<DiscussPost> page = elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                // 得到所有命中数据
                SearchHits hits = searchResponse.getHits();
                if (hits.getTotalHits() <= 0){
                    return null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits){
                    DiscussPost post = new DiscussPost();
                    // 得到每个被遍历数据的信息，然后设置post
                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮结果
                    // 得到title字段的高亮数据
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null){
                        // 只取第一个高亮
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null){
                        post.setTitle(contentField.getFragments()[0].toString());
                    }
                    list.add(post);
                }
                // 对返回数据进行初始化，传入list，其他都是固定的（目前）
                return new AggregatedPageImpl(list,pageable,
                        hits.getTotalHits(), searchResponse.getAggregations(),
                        searchResponse.getScrollId(), hits.getMaxScore());
            }
        });

        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page){
            System.out.println(post);
        }

    }
}

// localhost:9200/discusspost/_doc/231 按id搜索
// localhost:9200/discusspost/_search 全部搜索
