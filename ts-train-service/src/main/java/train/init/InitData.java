package train.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import train.entity.TrainType;
import train.repository.TrainTypeRepository;
import train.service.TrainService;

@Component
public class InitData implements CommandLineRunner
{
    @Autowired
    TrainService service;

    @Autowired
    TrainTypeRepository repository;

    @Override
    public void run(String... args) throws Exception
    {
        TrainType info = new TrainType();

        info.setId("GaoTieOne");
        info.setConfortClass(Integer.MAX_VALUE);
        info.setEconomyClass(Integer.MAX_VALUE);
        info.setAverageSpeed(250);
        // service.create(info, null);
        createCorrected(info);

        info.setId("GaoTieTwo");
        info.setConfortClass(Integer.MAX_VALUE);
        info.setEconomyClass(Integer.MAX_VALUE);
        info.setAverageSpeed(200);
        // service.create(info, null);
        createCorrected(info);

        info.setId("DongCheOne");
        info.setConfortClass(Integer.MAX_VALUE);
        info.setEconomyClass(Integer.MAX_VALUE);
        info.setAverageSpeed(180);
        // service.create(info, null);
        createCorrected(info);

        info.setId("ZhiDa");
        info.setConfortClass(Integer.MAX_VALUE);
        info.setEconomyClass(Integer.MAX_VALUE);
        info.setAverageSpeed(120);
        // service.create(info, null);
        createCorrected(info);

        info.setId("TeKuai");
        info.setConfortClass(Integer.MAX_VALUE);
        info.setEconomyClass(Integer.MAX_VALUE);
        info.setAverageSpeed(120);
        // service.create(info, null);
        createCorrected(info);

        info.setId("KuaiSu");
        info.setConfortClass(Integer.MAX_VALUE);
        info.setEconomyClass(Integer.MAX_VALUE);
        info.setAverageSpeed(90);
        // service.create(info, null);
        createCorrected(info);

        insertForRebook();
    }

    private void createCorrected(TrainType info) {
        repository.save(info);
    }

    private void insertForRebook() {
        TrainType info = new TrainType();

        info.setId("test");
        info.setConfortClass(0);
        info.setEconomyClass(0);
        info.setAverageSpeed(250);
        service.create(info, null);
    }
}
