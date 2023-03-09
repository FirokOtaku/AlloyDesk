package firok.spring.alloydesk.deskleg.service_multi;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.UserBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

import static firok.topaz.general.Collections.sizeOf;

@Service
public class UserMultiService
{
	@Autowired
	IService<UserBean> service;

	public String addUser(String username, String nickname, String password, byte[] avatar)
	{
		var bean = new UserBean();
		bean.setUsername(username);
		bean.setNickname(nickname);
		bean.setPassword(password);
		bean.setAvatar(avatar);
		bean.setIsDisable(false);
		service.save(bean);
		return bean.getId();
	}

	@SafeVarargs
	private static <T> SFunction<T, ?>[] Ts(SFunction<T, ?>... ts)
	{
		return ts;
	}
	private static final SFunction<UserBean, ?>[] selectWithAvatar = Ts(
			UserBean::getId,
			UserBean::getUsername,
			UserBean::getPassword,
			UserBean::getNickname,
			UserBean::getIsDisable
	);
	private static final SFunction<UserBean, ?>[] selectWithoutAvatar = Ts(
			UserBean::getId,
			UserBean::getUsername,
			UserBean::getPassword,
			UserBean::getNickname,
			UserBean::getAvatar,
			UserBean::getIsDisable
	);
	public UserBean getUser(String id, boolean withAvatar)
	{
		var qw = new QueryWrapper<UserBean>().lambda()
				.select(withAvatar ? selectWithAvatar : selectWithoutAvatar)
				.eq(UserBean::getId, id);
		var ret = service.list(qw);
		return sizeOf(ret) == 1 ? ret.get(0) : null;
	}

	/**
	 * 如果某个用户想改名, 检查给定用户名是否可用
	 * */
	private boolean checkNameForUser(String id, String username, String nickname)
	{
		return false;
	}
	public void updateUser(UserBean user)
	{
		service.updateById(user);
	}
	public void deleteUser(String id)
	{
		var user = new UserBean();
		user.setId(id);
		user.setIsDisable(true);
		service.updateById(user);
	}
}
