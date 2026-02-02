package fr.dinum.beta.gouv.doctorat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AngularForwardController {

	@RequestMapping(value = { "/", "/{path:^(?!api|static|.*\\..*).*}" })
	public String forward() {
		return "forward:/index.html";
	}
}
