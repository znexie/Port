package ship;

import org.apache.log4j.Logger;
import port.Berth;
import port.Port;
import port.PortException;
import warehouse.Container;
import warehouse.Warehouse;

import java.util.List;
import java.util.Random;

public class Ship implements Runnable {

	private final static Logger logger = Logger.getLogger("console");
	private volatile boolean stopThread = false;

	private String name;
	private Port port;
	private Warehouse shipWarehouse;

	public Ship(String name, Port port, int shipWarehouseSize) {
		this.name = name;
		this.port = port;
		shipWarehouse = new Warehouse(shipWarehouseSize);
	}

	public void setContainersToWarehouse(List<Container> containerList) {
		shipWarehouse.addContainer(containerList);
	}

	public String getName() {
		return name;
	}

	public void stopThread() {
		stopThread = true;
	}

	public void run() {
		try {
			while (!stopThread) {
				atSea();
				inPort();
			}
		} catch (InterruptedException e) {
			logger.error("С кораблем случилась неприятность и он уничтожен.", e);
		} catch (PortException e) {
			logger.error("С кораблем случилась неприятность и он уничтожен.", e);//!!! переписать сообщение
		}
	}

	private void atSea() throws InterruptedException {
		Thread.sleep(500);
	}


	private void inPort() throws PortException, InterruptedException {

		boolean isLockedBerth = false;
		Berth berth = null;
		try {
			isLockedBerth = port.lockBerth(this);

			if (isLockedBerth) {
				berth = port.getBerth(this);
				logger.debug("Корабль " + name + " пришвартовался к причалу " + berth.getId());
				ShipAction action = getNextAction();
				executeAction(action, berth);
			} else {
				logger.debug("Кораблю " + name + " отказано в швартовке к причалу ");
			}
		} finally {
			if (isLockedBerth){
				Thread.sleep(1000);
				port.unlockBerth(this);
				logger.debug("Корабль " + name + " отошел от причала " + berth.getId());
			}
		}

	}

	private void executeAction(ShipAction action, Berth berth) throws InterruptedException {
		switch (action) {
		case LOAD_TO_PORT:
 				loadToPort(berth);
			break;
		case LOAD_FROM_PORT:
				loadFromPort(berth);
			break;
		}
	}

	private boolean loadToPort(Berth berth) throws InterruptedException {

		int containersNumberToMove = conteinersCount(port.getPortWarehouse().getRealSize());
		boolean result = false;

		logger.debug("Корабль " + name + " хочет загрузить " + containersNumberToMove
				+ " контейнеров на склад порта.");

		result = berth.add(shipWarehouse, containersNumberToMove);
		
		if (!result) {
			logger.debug("Недостаточно места на складе порта для выгрузки кораблем "
					+ name + " " + containersNumberToMove + " контейнеров.");
		} else {
			logger.debug("Корабль " + name + " выгрузил " + containersNumberToMove
					+ " контейнеров в порт.");
			
		}
		return result;
	}

	private boolean loadFromPort(Berth berth) throws InterruptedException {
		
		int containersNumberToMove = conteinersCount(this.getWarehouse().getRealSize());
		
		boolean result = false;

		logger.debug("Корабль " + name + " хочет загрузить " + containersNumberToMove
				+ " контейнеров со склада порта.");
		
		result = berth.get(shipWarehouse, containersNumberToMove);
		
		if (result) {
			logger.debug("Корабль " + name + " загрузил " + containersNumberToMove
					+ " контейнеров из порта.");
		} else {
			logger.debug("Недостаточно места на на корабле " + name
					+ " для погрузки " + containersNumberToMove + " контейнеров из порта.");
		}
		
		return result;
	}

	private int conteinersCount(int size) {
		Random random = new Random();

		if (size == 0) {
			return 1;
		}

		int containers = random.nextInt(size) + 1;
		if (containers > size) {
			return size;
		}

		return containers;
	}

	private ShipAction getNextAction() {
		Random random = new Random();
		int value = random.nextInt(4000);
		if (value < 1000 && this.shipWarehouse.getRealSize() != 0) {
			return ShipAction.LOAD_TO_PORT;
		} else if (value < 2000) {
			return ShipAction.LOAD_FROM_PORT;
		}
		return ShipAction.LOAD_TO_PORT;
	}

	enum ShipAction {
		LOAD_TO_PORT, LOAD_FROM_PORT
	}

	public Warehouse getWarehouse() {return shipWarehouse;}

	public void waitForDispatch()
	{
		try {
			Thread.sleep(200 );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
