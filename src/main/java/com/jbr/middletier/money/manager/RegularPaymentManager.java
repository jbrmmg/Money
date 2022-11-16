package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.dataaccess.RegularRepository;
import com.jbr.middletier.money.dto.RegularDTO;
import com.jbr.middletier.money.exceptions.InvalidRegularIdException;
import com.jbr.middletier.money.exceptions.RegularAlreadyExistsException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class RegularPaymentManager {
    private static final Logger LOG = LoggerFactory.getLogger(RegularPaymentManager.class);

    private final RegularRepository regularRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public RegularPaymentManager(RegularRepository regularRepository, ModelMapper modelMapper) {
        this.regularRepository = regularRepository;
        this.modelMapper = modelMapper;
    }

    public Iterable<RegularDTO> getRegularPayments() {
        List<RegularDTO> result = new ArrayList<>();

        for(Regular regular : regularRepository.findAll()) {
            result.add(modelMapper.map(regular, RegularDTO.class));
        }

        return result;
    }

    public void createRegularPayment(RegularDTO regular) throws RegularAlreadyExistsException {
        LOG.info("Create regular payment.");

        // Make sure this id is null.
        if(regular.getId() != null) {
            throw new RegularAlreadyExistsException(regular);
        }

        regularRepository.save(modelMapper.map(regular, Regular.class));
    }

    public void updateRegularPayment(RegularDTO regular) throws InvalidRegularIdException {
        LOG.info("Update regular payment.");

        Optional<Regular> existingRegular = regularRepository.findById(regular.getId());

        if(existingRegular.isPresent()) {
            regularRepository.save(modelMapper.map(regular,Regular.class));
        } else {
            throw new InvalidRegularIdException(regular);
        }
    }

    public void deleteRegularPayment(RegularDTO regular) throws InvalidRegularIdException {
        LOG.info("Delete regular payment.");

        Optional<Regular> existingRegular = regularRepository.findById(regular.getId());

        if(existingRegular.isPresent()) {
            regularRepository.deleteById(existingRegular.get().getId());
        } else {
            throw new InvalidRegularIdException(regular);
        }
    }
}
