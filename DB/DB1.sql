-- generic
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (57,'HeightOfRoom','Stilt Floor',38,7);
	
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (58,'HeightOfRoom','Service Floor',39,7);
	
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (59,'HeightOfRoom','MEP Room',30,7);

INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (60,'HeightOfRoom','Laundry Room',31,7);

INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (61,'HeightOfRoom','Generator Room',36,7);

INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (62,'HeightOfRoom','Lift Lobby',32,7);

INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (63,'HeightOfRoom','CCTV Room',28,7);
	 
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (64,'HeightOfRoom','Service Room',29,7);
	

-- Passage layername changed
update state.egdcr_layername  set value = 'PASSAGE_DOUBLELOADED' where "key" ='LAYER_NAME_PASSAGE_STAIR';
