package firok.spring.alloydesk.deskleg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import firok.topaz.spring.Ret;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.FileSystem;
import oshi.software.os.OperatingSystem;

import java.util.*;

@RestController
@RequestMapping("/system")
@CrossOrigin
public class SystemController
{
	private static final SystemInfo sys = new SystemInfo();
	private static final OperatingSystem os;
	private static final FileSystem filesystem;
	private static final HardwareAbstractionLayer hardware;
	private static final CentralProcessor processor;
	private static final GlobalMemory mem;
	private static final List<GraphicsCard> graphicsCards;
	private static final List<HWDiskStore> diskStores;
	private static final ObjectNode InfoStatic;
	static
	{
		var om = new ObjectMapper();
		InfoStatic = om.createObjectNode();

		os = sys.getOperatingSystem();
		InfoStatic.put("osName", os.getFamily());
		InfoStatic.put("osVersion", os.getVersionInfo().toString());
		InfoStatic.put("osBitness", os.getBitness());
		InfoStatic.put("osManufacturer", os.getManufacturer());
		InfoStatic.put("osFamily", os.getFamily());
		
		filesystem = os.getFileSystem();
		var fss = om.createArrayNode();
		for(var store : filesystem.getFileStores())
		{
			var fs = om.createObjectNode();
			fs.put("name", store.getName());
			fs.put("type", store.getType());
			fs.put("description", store.getDescription());
			fs.put("label", store.getLabel());
			fs.put("logicalVolume", store.getLogicalVolume());
			fs.put("uuid", store.getUUID());
			fs.put("volume", store.getVolume());
			fs.put("totalSpace", store.getTotalSpace());
			fs.put("totalInodes", store.getTotalInodes());
			fss.add(fs);
		}
		InfoStatic.set("fss", fss);

		hardware = sys.getHardware();

		mem = hardware.getMemory();
		InfoStatic.put("memTotal", mem.getTotal());
		InfoStatic.put("memPage", mem.getPageSize());

		processor = hardware.getProcessor();
		InfoStatic.put("coreLogicalCount", processor.getLogicalProcessorCount());
		InfoStatic.put("corePhysicalCount", processor.getPhysicalProcessorCount());
		InfoStatic.put("corePhysicalPackage", processor.getPhysicalPackageCount());
		InfoStatic.put("coreMaxFreq", processor.getMaxFreq());
		var cores = om.createArrayNode();
		for(var logicalProcessor : processor.getLogicalProcessors())
		{
			var core = om.createObjectNode();
			core.put("numaCode", logicalProcessor.getNumaNode());
			core.put("processorGroup", logicalProcessor.getProcessorGroup());
			core.put("processorNumber", logicalProcessor.getProcessorNumber());
			core.put("physicalProcessorNumber", logicalProcessor.getPhysicalProcessorNumber());
			core.put("physicalPackageNumber", logicalProcessor.getPhysicalPackageNumber());
			cores.add(core);
		}
		InfoStatic.set("cores", cores);

		graphicsCards = hardware.getGraphicsCards();
		var gpus = om.createArrayNode();
		for(var graphicsCard : graphicsCards)
		{
			var gpu = om.createObjectNode();
			gpu.put("name", graphicsCard.getName());
			gpu.put("deviceId", graphicsCard.getDeviceId());
			gpu.put("vram", graphicsCard.getVRam());
			gpu.put("version", graphicsCard.getVersionInfo());
			gpu.put("vendor", graphicsCard.getVendor());
			gpus.add(gpu);
		}
		InfoStatic.set("gpus", gpus);

		diskStores = hardware.getDiskStores();
		var disks = om.createArrayNode();
		for(var diskStore : diskStores)
		{
			var disk = om.createObjectNode();
			disk.put("name", diskStore.getName());
			disk.put("model", diskStore.getModel());
			disk.put("serial", diskStore.getSerial());
			disk.put("size", diskStore.getSize());

			var diskPartitions = diskStore.getPartitions();
			var partitions = om.createArrayNode();
			for(var diskPartition : diskPartitions)
			{
				var partition = om.createObjectNode();
				partition.put("size", diskPartition.getSize());
				partition.put("name", diskPartition.getName());
				partition.put("identification", diskPartition.getIdentification());
				partition.put("major", diskPartition.getMajor());
				partition.put("minor", diskPartition.getMinor());
				partition.put("type", diskPartition.getType());
				partition.put("mountPoint", diskPartition.getMountPoint());
				partition.put("uuid", diskPartition.getUuid());
				partitions.add(partition);
			}
			disk.set("partitions", partitions);
			disks.add(disk);
		}
		InfoStatic.set("disks", disks);
	}

	@GetMapping("/static")
	public Ret<?> getStaticInfo()
	{
		return Ret.success(InfoStatic);
	}

	@GetMapping("/dynamic")
	public Ret<?> getDynamicInfo()
	{
		var om = new ObjectMapper();
		var ret = om.createObjectNode();
		ret.put("memAvailable", mem.getAvailable());


		return Ret.success(ret);
	}
}
