package com.nowcoder.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CommunityApplication {

	@PostConstruct
	public void init(){
		// 解决netty启动冲突问题
		// Netty4Utils.setAvailableProcessors()
		System.setProperty("es.set.netty.runtime.available.processors","false");
	}


	//启动Tomcat和创建Spring容器，扫描配置类所在的包和子包的有注解的bean装配到容器里
	//CommunityApplication.class表示类的配置文件
	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
}

}

//数据库访问用@Repository注解
//@Component哪里都可以用
//bean只被实例化一次，通过@Scope("prototype")可以创建多个实例

// redis是一款基于键值对的NoSQL数据库，它的value支持字符串strings、哈希hashes、列表lists
// 集合set、有序集合sorted set
// redis将所有数据都存放在内存内，同时还能就内存中的数据以快照或者日志（AOF，实时，体积大，恢复速度慢）的方式保存到硬盘上
// redis典型应用场景：缓存、排行榜、计数器、社交网络、消息队列