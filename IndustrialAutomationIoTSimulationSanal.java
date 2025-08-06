package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class IndustrialAutomationIoTSimulationSanal {

    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();
    static double SENSOR_TRANSMISSION_TIME = 2.5;

    public static void main(String[] args) {
        Log.printLine("Starting Industrial Automation IoT Simulation...");
        try {
            Log.disable();
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "industrial-automation-monitoring";
            FogBroker broker = new FogBroker("broker");
            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            createFogDevices(broker.getId(), appId);

            Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
            moduleMapping.addModuleToDevice("sensorModule", "edge-device");
            moduleMapping.addModuleToDevice("processingModule", "fog-node");
            moduleMapping.addModuleToDevice("cloudModule", "cloud");

            controller.submitApplication(application, new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping));
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            Log.printLine("Simulation finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happened");
        }
    }

    private static void createFogDevices(int userId, String appId) {
        FogDevice cloud = createFogDevice("cloud", 22000, 40000, 20000, 20000, 0, 0.012, 2100.0, 1800.0);
        cloud.setParentId(-1);
        cloud.setUplinkLatency(100);
        fogDevices.add(cloud);

        FogDevice fogNode = createFogDevice("fog-node", 8000, 16000, 4000, 4000, 1, 0.004, 150.0, 100.0);
        fogNode.setParentId(cloud.getId());
        fogNode.setUplinkLatency(8);
        fogDevices.add(fogNode);

        FogDevice edgeDevice = createFogDevice("edge-device", 4000, 8000, 2000, 2000, 2, 0.001, 120.0, 85.0);
        edgeDevice.setParentId(fogNode.getId());
        edgeDevice.setUplinkLatency(4);
        fogDevices.add(edgeDevice);

        addSensorsAndActuators(edgeDevice.getId(), userId, appId);
    }

    private static void addSensorsAndActuators(int parentId, int userId, String appId) {
        sensors.add(new Sensor("s-pressure", "PRESSURE", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)));
        sensors.add(new Sensor("s-vibration", "VIBRATION", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)));
        sensors.add(new Sensor("s-flow", "FLOW_RATE", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)));
        sensors.add(new Sensor("s-torque", "TORQUE", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)));
        sensors.add(new Sensor("s-speed", "MOTOR_SPEED", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)));

        for (Sensor sensor : sensors) {
            sensor.setGatewayDeviceId(parentId);
            sensor.setLatency(0.8);
        }

        Actuator controller = new Actuator("a-controller", userId, appId, "MACHINE_CONTROL");
        controller.setGatewayDeviceId(parentId);
        controller.setLatency(0.8);
        actuators.add(controller);
    }

    private static FogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw,
                                             int level, double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 750000;
        int bw = 15000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                "x86", "Linux", "Xen", host, 10.0, 3.0, 0.05, 0.001, 0.0);

        FogDevice device = null;
        try {
            device = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), new LinkedList<Storage>(), 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        device.setLevel(level);
        return device;
    }

    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("sensorModule", 15);
        application.addAppModule("processingModule", 18);
        application.addAppModule("cloudModule", 25);

        application.addAppEdge("PRESSURE", "sensorModule", 3000, 1400, "PRESSURE", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("VIBRATION", "sensorModule", 3000, 1400, "VIBRATION", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("FLOW_RATE", "sensorModule", 3000, 1400, "FLOW_RATE", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("TORQUE", "sensorModule", 3000, 1400, "TORQUE", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("MOTOR_SPEED", "sensorModule", 3000, 1400, "MOTOR_SPEED", Tuple.UP, AppEdge.SENSOR);

        application.addAppEdge("sensorModule", "processingModule", 2500, 1800, "_SENSOR_TO_PROCESSING_", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("processingModule", "cloudModule", 2200, 1600, "_PROCESSING_TO_CLOUD_", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("cloudModule", "MACHINE_CONTROL", 1800, 1200, "MACHINE_CONTROL", Tuple.DOWN, AppEdge.ACTUATOR);

        for (String sensor : new String[]{"PRESSURE", "VIBRATION", "FLOW_RATE", "TORQUE", "MOTOR_SPEED"}) {
            application.addTupleMapping("sensorModule", sensor, "_SENSOR_TO_PROCESSING_", new FractionalSelectivity(1.0));
        }
        application.addTupleMapping("processingModule", "_SENSOR_TO_PROCESSING_", "_PROCESSING_TO_CLOUD_", new FractionalSelectivity(1.0));
        application.addTupleMapping("cloudModule", "_PROCESSING_TO_CLOUD_", "MACHINE_CONTROL", new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("PRESSURE");
            add("sensorModule");
            add("processingModule");
            add("cloudModule");
            add("MACHINE_CONTROL");
        }});

        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
        }};
        application.setLoops(loops);

        return application;
    }
}
