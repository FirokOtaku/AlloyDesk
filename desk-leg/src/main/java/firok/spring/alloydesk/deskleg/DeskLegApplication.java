package firok.spring.alloydesk.deskleg;

import firok.spring.alloydesk.deskleg.service_multi.LogMultiService;
import firok.spring.dbsculptor.DirectMapper;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.dbsculptor.DubniumSculptor;
import firok.spring.mvci.runtime.CurrentMappers;
import firok.topaz.Topaz;
import firok.topaz.general.Version;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.temporal.ChronoUnit;


@EnableScheduling
@EnableTransactionManagement
@MapperScans({
		@MapperScan("firok.spring.alloydesk.deskleg.mapper"),
		@MapperScan("firok.spring.dbsculptor"),
})
@ComponentScans({
		@ComponentScan("firok.spring.alloydesk.deskleg.config"),
		@ComponentScan("firok.spring.alloydesk.deskleg.mapper"),
		@ComponentScan("firok.spring.alloydesk.deskleg.service"),
		@ComponentScan("firok.spring.alloydesk.deskleg.service_impl"),
		@ComponentScan("firok.spring.alloydesk.deskleg.controller"),
		@ComponentScan("firok.spring.mvci.runtime"),
		@ComponentScan("firok.spring.dbsculptor"),
})
@SpringBootApplication
public class DeskLegApplication
{
	public static final String NAME = "Alloy Desk";
	public static final Version VERSION = new Version(0, 15, 0);
	public static final String AUTHOR = "Firok";
	public static final String LINK = "https://github.com/FirokOtaku/AlloyDesk";

	@Autowired
	LogMultiService logger;
	private long timestampStart;
	@PreDestroy
	private void preDestroy()
	{
		var now = System.currentTimeMillis();
		var interval = java.time.Duration.of(now - timestampStart, ChronoUnit.MILLIS);
		logger.systemKeypoint("系统停止 (last: %s)".formatted(interval));
	}
	@PostConstruct
	private void postConstruct()
	{
		logger.systemKeypoint("系统启动 (version: %s)".formatted(VERSION));
		timestampStart = System.currentTimeMillis();
	}

	public static void main(String[] args) throws Exception
	{
		SpringApplication.run(DeskLegApplication.class, args);
	}

}
