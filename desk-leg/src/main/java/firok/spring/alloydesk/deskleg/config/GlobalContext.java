package firok.spring.alloydesk.deskleg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.locks.ReentrantLock;

@Configuration
public class GlobalContext
{
	@Bean
	public ReentrantLock GlobalLock()
	{
		return new ReentrantLock();
	}
}
