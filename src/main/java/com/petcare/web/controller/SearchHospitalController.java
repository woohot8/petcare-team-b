package com.petcare.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.web.domains.HosInfoCodeVo;
import com.petcare.web.domains.HospitalVo;
import com.petcare.web.domains.ReviewBrdVo;
import com.petcare.web.mapper.HospitalSearchMapper;
import com.petcare.web.mapper.ReviewBrdMapper;
import com.petcare.web.utills.HospitalCrawlingProxy;
import com.petcare.web.utills.Proxy;

@Controller
@RequestMapping("/sch/*")
public class SearchHospitalController {
	@Autowired HospitalSearchMapper hospitalSearchMapper;
	@Autowired ReviewBrdMapper reviewBrdMapper;
	@Autowired HospitalCrawlingProxy hospitalCrawlingProxy;
	@Autowired Proxy pxy;  // pagenation 및 검색어 VO
	

	
	@GetMapping("/")  
	public @ResponseBody Map<String,Object> selectHosInfoCode() throws Exception{
		Map<String, Object> map = new HashMap<String, Object>();
		ArrayList<HosInfoCodeVo> code = hospitalSearchMapper.selectHosInfoCode();
		ArrayList<HosInfoCodeVo> tempforAnimal = new ArrayList<HosInfoCodeVo>();
		ArrayList<HosInfoCodeVo> tempforTime = new ArrayList<HosInfoCodeVo>();
		ArrayList<HosInfoCodeVo> tempforSubject = new ArrayList<HosInfoCodeVo>();
		ArrayList<HosInfoCodeVo> tempforEtc = new ArrayList<HosInfoCodeVo>();
		ArrayList<HosInfoCodeVo> tempforConvenience = new ArrayList<HosInfoCodeVo>();
		ObjectMapper mapper = new ObjectMapper();		
		for (HosInfoCodeVo hosInfoCodeVo : code) {
			if(hosInfoCodeVo.getHosInfoCode() / 100 == 1 ) {
				tempforAnimal.add(hosInfoCodeVo);
			}else if(hosInfoCodeVo.getHosInfoCode() / 100 == 2 ) {
				tempforTime.add(hosInfoCodeVo);
			}else if(hosInfoCodeVo.getHosInfoCode() / 100 == 3 ) {
				tempforSubject.add(hosInfoCodeVo);
			}else if(hosInfoCodeVo.getHosInfoCode() / 100 == 4 ) {
				tempforEtc.add(hosInfoCodeVo);
			}else if(hosInfoCodeVo.getHosInfoCode() / 100 == 5 ) {
				tempforConvenience.add(hosInfoCodeVo);
			}
		}

		map.put("animal", mapper.writeValueAsString(tempforAnimal));
		map.put("time", mapper.writeValueAsString(tempforTime));
		map.put("subject", mapper.writeValueAsString(tempforSubject));
		map.put("etc", mapper.writeValueAsString(tempforEtc));
		map.put("convenience", mapper.writeValueAsString(tempforConvenience));
		
		return map;
		
	}
	
	@PostMapping("/hospitalList/{pageNo}")  
	public @ResponseBody Map<String,Object> selectAllHospitalList(@PathVariable String pageNo, @RequestBody List<String> hosinfolist ){
		Map<String, Object> map = new HashMap<String, Object>();
		Function<Proxy,List<HospitalVo>> c;
		if (hosinfolist.size() != 0) {
			pxy.setCheckBoxList(hosinfolist);
			pxy.setPageNo(Integer.parseInt(pageNo));
			Function<Proxy,Integer> n = h -> hospitalSearchMapper.countHospitalByCondition(h);
			pxy.paging(n.apply(pxy)); 
			 c = t -> hospitalSearchMapper.selectHospitalList(t);

		}else {
			Supplier<Integer> n = () -> hospitalSearchMapper.countAllHospital();
			pxy.setPageNo(Integer.parseInt(pageNo));
			pxy.paging(n.get());
			c = t -> hospitalSearchMapper.selectHospitalAllList(t);
		}	
		map.put("result", c.apply(pxy));
		map.put("pagination", pxy);
		map.put("msg","SUCCESS");
		return map;
	}
	
	@GetMapping("/detail")  
	public String selectHospitalDetail(@RequestParam("hosNo") String hosNo, Model model){
		Function<String,HospitalVo> n = t -> hospitalSearchMapper.selectHospitalDetail(t);
		model.addAttribute("result",n.apply(hosNo));
		
		return "hospitalDetail";
		
	}
	
	@GetMapping("/{hosNo}/pageNo/{pageNo}")  
	public @ResponseBody Map<String,Object> selectAllReview(@PathVariable String hosNo, @PathVariable String pageNo){
		Map<String, Object> tempMap =new HashMap<String, Object>();
		Function<String,Integer> c = t -> reviewBrdMapper.cntReview(t);		
		pxy.setHosNo(Integer.parseInt(hosNo));
		pxy.setPageNo(Integer.parseInt(pageNo));
		pxy.paging(c.apply(hosNo));
		
		Function<Proxy,List<ReviewBrdVo>> r = t -> reviewBrdMapper.selectReview(t);
		tempMap.put("review", r.apply(pxy));
		tempMap.put("pagenation", pxy);
		
		return tempMap;
		
	}
	
	@GetMapping("/crawling")
	public @ResponseBody Map<String,Object> crawlingAllHospitalDB(){
		Map<String, Object> tempMap =new HashMap<String, Object>();
		Consumer<HospitalVo> t = s -> hospitalSearchMapper.insetHospitalDumpData(s);
		
		try {
			for (HospitalVo tempHosDetailDb : hospitalCrawlingProxy.animal()) {
				t.accept(tempHosDetailDb);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		tempMap.put("msg", "SUCCESS"); 
		
		return tempMap;
		
	}
	
	
}
