package firok.spring.alloydesk.deskleg.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JsonConfig
{
	@Bean
	public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
		return builder -> builder.serializerByType(Page.class, new JsonSerializer<Page>()
		{
			@Override
			public void serialize(Page value, JsonGenerator gen, SerializerProvider serializers) throws IOException
			{
				if(value == null)
				{
					gen.writeNull();
					return;
				}
				gen.writeStartObject();
				gen.writeNumberField("pageIndex", value.getCurrent());
				gen.writeNumberField("pageSize", value.getSize());
				gen.writeNumberField("count", value.getTotal());
				gen.writeNumberField("pageCount", value.getPages());
				gen.writeObjectField("records", value.getRecords());
				gen.writeEndObject();
			}
		});
	}
}
