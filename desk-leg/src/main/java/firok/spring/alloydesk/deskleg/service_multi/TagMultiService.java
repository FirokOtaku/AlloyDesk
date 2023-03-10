package firok.spring.alloydesk.deskleg.service_multi;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import firok.spring.alloydesk.deskleg.bean.TagBean;
import firok.spring.alloydesk.deskleg.bean.TagTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagMultiService
{
	@Autowired
	IService<TagBean> service;

	public Set<String> getTagValues(TagTypeEnum type, String targetId)
	{
		var qwTagId = new QueryWrapper<TagBean>().lambda()
				.select(TagBean::getTagValue)
				.eq(TagBean::getTagType, type)
				.eq(TagBean::getTargetId, targetId);
		var list = service.list(qwTagId);
		return list.stream().map(TagBean::getTagValue).collect(Collectors.toSet());
	}
	public Map<String, Set<String>> getTagValues(TagTypeEnum type, Collection<String> targetIds)
	{
		var qwTagId = new QueryWrapper<TagBean>().lambda()
				.select(TagBean::getTargetId, TagBean::getTagValue)
				.eq(TagBean::getTagType, type)
				.in(TagBean::getTargetId, targetIds);
		var list = service.list(qwTagId);
		return firok.topaz.general.Collections.mappingKeyMultiValueSet(
				list,
				TagBean::getTargetId,
				TagBean::getTagValue
		);
	}

	public void setTagValues(String targetId, TagTypeEnum type, String... values)
	{
		setTagValues(targetId, type, values == null || values.length == 0 ? null : Arrays.asList(values));
	}
	public void setTagValues(String targetId, TagTypeEnum type, Collection<String> values)
	{
		var qw = new QueryWrapper<TagBean>().lambda()
				.eq(TagBean::getTagType, type)
				.eq(TagBean::getTargetId, targetId);
		service.remove(qw);
		if(values == null || values.isEmpty()) return;
		var now = new Date();
		var beans = values.stream().map(value -> {
			var bean = new TagBean();
			bean.setId(UUID.randomUUID().toString());
			bean.setTargetId(targetId);
			bean.setTagValue(value);
			bean.setTagType(type);
			bean.setCreateUserId(""); // todo
			bean.setCreateTimestamp(now);
			return bean;
		}).toList();
		service.saveBatch(beans);
	}
}
