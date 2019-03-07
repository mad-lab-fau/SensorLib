package de.fau.sensorlib.rx;

import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.AbstractSensor;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class RxSensorDataProcessor extends SensorDataProcessor
{

    public enum SensorState
    {
        CREATED, CONNECTING, CONNECTED, DISCONNECTED, CONNECTION_LOST, STREAMING_STARTED, STREAMING_STOPPED
    }

    public static class SensorNotification<T>
    {
        private final AbstractSensor sensor;
        private final T notification;

        public SensorNotification(AbstractSensor sensor, T notification)
        {
            this.sensor = sensor;
            this.notification = notification;
        }

        public AbstractSensor getSensor()
        {
            return sensor;
        }

        public T getNotification()
        {
            return notification;
        }
    }

    private PublishSubject<SensorNotification<SensorState>> sensorStateSubject = PublishSubject.create();
    private PublishSubject<SensorNotification<Double>> sensorSamplingRateSubject = PublishSubject.create();
    private PublishSubject<SensorNotification<Object>> sensorNotificationSubject = PublishSubject.create();
    private PublishSubject<SensorDataFrame> newDataSubject = PublishSubject.create();


    @Override
    public void onSensorCreated(AbstractSensor sensor)
    {
        sensorStateSubject.onNext(new SensorNotification<SensorState>(sensor, SensorState.CREATED));
    }

    @Override
    public void onConnected(AbstractSensor sensor)
    {
        sensorStateSubject.onNext(new SensorNotification<SensorState>(sensor, SensorState.CONNECTED));
    }

    @Override
    public void onConnecting(AbstractSensor sensor)
    {
        sensorStateSubject.onNext(new SensorNotification<SensorState>(sensor, SensorState.CONNECTING));
    }

    @Override
    public void onDisconnected(AbstractSensor sensor)
    {
        sensorStateSubject.onNext(new SensorNotification<SensorState>(sensor, SensorState.DISCONNECTED));
    }

    @Override
    public void onConnectionLost(AbstractSensor sensor)
    {
        sensorStateSubject.onNext(new SensorNotification<SensorState>(sensor, SensorState.CONNECTION_LOST));
    }

    @Override
    public void onStartStreaming(AbstractSensor sensor)
    {
        sensorStateSubject.onNext(new SensorNotification<SensorState>(sensor, SensorState.STREAMING_STARTED));
    }

    @Override
    public void onStopStreaming(AbstractSensor sensor)
    {
        sensorStateSubject.onNext(new SensorNotification<SensorState>(sensor, SensorState.STREAMING_STOPPED));
    }

    @Override
    public void onSamplingRateChanged(AbstractSensor sensor, double newSamplingRate)
    {
        sensorSamplingRateSubject.onNext(new SensorNotification<Double>(sensor, newSamplingRate));
    }

    @Override
    public void onNotify(AbstractSensor sensor, Object notification)
    {
        sensorNotificationSubject.onNext(new SensorNotification<Object>(sensor, notification));
    }

    @Override
    public void onNewData(SensorDataFrame data)
    {
        newDataSubject.onNext(data);
    }

    public Observable<SensorNotification<SensorState>> getSensorStateObservable()
    {
        return sensorStateSubject;
    }

    public Observable<SensorNotification<Double>> getSensorSamplingRateObservable()
    {
        return sensorSamplingRateSubject;
    }

    public Observable<SensorNotification<Object>> getSensorNotificationObservable()
    {
        return sensorNotificationSubject;
    }

    public <T extends SensorDataFrame> Observable<T> getNewDataObservable() {
        return (Observable<T>) newDataSubject;
    }
}
