package firok.spring.alloydesk.deskleg;

import firok.spring.dbsculptor.DirectMapper;
import firok.spring.dbsculptor.Dubnium;
import firok.spring.dbsculptor.DubniumSculptor;
import firok.spring.mvci.runtime.CurrentMappers;
import firok.topaz.general.Version;
import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.transaction.annotation.EnableTransactionManagement;


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
	public static final Version VERSION = new Version(0, 5, 0);

	public static void main(String[] args) throws Exception
	{
		SpringApplication.run(DeskLegApplication.class, args);
	}

}
