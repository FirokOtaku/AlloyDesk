package firok.spring.alloydesk.deskleg.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("firok.spring.dreamspeak.mapper")
public class MybatisPlusConfig
{
    @Value("${spring.datasource.url}")
    String url;

    /**
     * 分页插件
     *
     * https://baomidou.com/pages/2976a3/#spring-boot
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor()
    {
        final DbType type;
        if(url.startsWith("jdbc:mysql"))
            type = DbType.MYSQL;
        else if(url.startsWith("jdbc:h2"))
            type = DbType.H2;
        else
            throw new IllegalArgumentException("不支持的目标数据库: " + url);

        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(type));
        return interceptor;
    }
}
