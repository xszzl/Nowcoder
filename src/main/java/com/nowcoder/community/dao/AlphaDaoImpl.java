package com.nowcoder.community.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository("alpha")    //字符串为bean的名字
@Primary    //表示有更高的优先级
public class AlphaDaoImpl implements AlphaDao {
    @Override
    public String alpha() {
        return "123";
    }
}
