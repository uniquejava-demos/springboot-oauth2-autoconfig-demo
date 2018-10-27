package cyper.demo.springoauth2auto.controllers;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cyper.demo.springoauth2auto.model.Todo;

@RestController
@RequestMapping("/api")
public class TodoController {

	@GetMapping("/todos")
	public List<Todo> listTodos() {
		Todo todo1 = new Todo("1", "write code", false);
		Todo todo2 = new Todo("2", "write more code", false);
		return Arrays.asList(todo1, todo2);
	}
}
