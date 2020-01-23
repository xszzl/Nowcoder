package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    // 替换符
    private static final String REPLACEMENT = "***";

    // 根节点初始化
    private TrieNode rootNode = new TrieNode();

    @PostConstruct  //这个注解表示这个方法是初始化方法，在Bean实例化时就会被调用，而Bean在服务启动时被初始化
    public void init(){
        // 编译之后所有文件都被编译到class中
        try (
                // 得到字节流
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                // 得到字符流，再转换为缓冲流
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ){
            String keyword;
            while ((keyword = reader.readLine())!= null){
                // 添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (IOException e){
            logger.error("加载敏感词文件失败:" +e.getMessage());
        }
    }

    /**
     * 过滤敏感词
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if (StringUtils.isBlank(text)){
            return null;
        }
        // 指向树的指针
        TrieNode tempNode = rootNode;
        // 指向敏感词开始的指针
        int begin = 0;
        // 指向敏感词当前扫描位置的指针
        int position = 0;
        // 过滤之后的结果
        StringBuilder sb = new StringBuilder();

        // 遍历过程是由begin来定位的，和node比较的是position指向的字符
        while (position<text.length()){

            char c = text.charAt(position);

            // 跳过符号
            if (isSymbol(c)){
                // 若树上的指针指向根节点，将此符号记入结果，让敏感词开始指针前进
                if (tempNode == rootNode){
                    sb.append(c);
                    begin++;
                }
                // 这个指针是一定会前进的，因为要跳过符号
                position++;
                continue;
            }

            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null){
                // 如果当前串不是敏感词串
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 重新指向根节点
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()){
                // 如果当前串是敏感词串,则替换该字符串
                sb.append(REPLACEMENT);
                // 进入下一个位置
                begin = ++position;
                // 重新指向根节点
                tempNode = rootNode;
            } else {
                // 检查下一个字符
                position++;
            }
        }

        // 将最后的字符记入结果
        sb.append(text.substring(begin));

        return sb.toString();
    }

    // 判断是否为符号
    private boolean isSymbol(Character c){
        // 0x2E80~0x9FFF为东亚文字
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    // 将一个敏感词添加到前缀树中区
    private void addKeyword(String keyword){
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++){
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if (subNode == null){
                // 初始化子节点
                subNode = new TrieNode();
                tempNode.addSubNode(c,subNode);
            }
            // 指向子节点进入下一轮循环
            tempNode = subNode;

            // 设置结束的标识
            if (i == keyword.length()-1){
                tempNode.setKeywordEnd(true);
            }
        }
    }

    // 定义前缀树
    private class TrieNode{
        // 是否是敏感词结束的标识
        private boolean isKeywordEnd = false;
        // 子节点(key是下级字符，value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        // 添加子节点
        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c,node);
        }

        // 获取子节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }
}
