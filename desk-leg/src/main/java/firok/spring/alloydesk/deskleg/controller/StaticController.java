package firok.spring.alloydesk.deskleg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
public class StaticController
{
	@RequestMapping("/")
	public String index()
	{
		return "forward:/index.html";
	}
}
