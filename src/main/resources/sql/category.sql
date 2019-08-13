CREATE TABLE `category` (
  `id` char(3) NOT NULL,
  `name` varchar(45) NOT NULL,
  `sort` int(11) NOT NULL,
  `restricted` char(1) NOT NULL,
  `expense` char(1) NOT NULL,
  `colour` char(6) NOT NULL,
  `groupid` varchar(45) NOT NULL,
  `system_use` char(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
