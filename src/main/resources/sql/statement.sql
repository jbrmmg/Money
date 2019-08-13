CREATE TABLE `statement` (
  `account` char(4) NOT NULL,
  `month` int(11) NOT NULL,
  `year` int(11) NOT NULL,
  `open_balance` decimal(10,2) DEFAULT NULL,
  `locked` char(1) DEFAULT NULL,
  PRIMARY KEY (`account`,`month`,`year`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
