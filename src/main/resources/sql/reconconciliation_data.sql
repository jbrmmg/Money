CREATE TABLE `reconciliation_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `category` char(3) NOT NULL,
  `date` datetime NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `description` varchar(40) NOT NULL,
  `colour` char(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=latin1;
