/* INSERT QUERY NO: 164 */
INSERT INTO generic.egbpa_sub_occupancy(id, code, name, ordernumber, isactive, createdby, createddate, lastmodifieddate, lastmodifiedby, version, maxcoverage, minfar, maxfar, occupancy, description,colorcode)
VALUES 
(164, 'AF-AWWS', 'Accommodation of watch and ward/maintenance staff', 3, 't', 1, now(), NULL, 1, 0, 225, 163, 164, (select id from generic.egbpa_occupancy where code='AF'), 'Accommodation of watch and ward/maintenance staff', 104);