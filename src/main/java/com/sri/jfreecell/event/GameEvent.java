package com.sri.jfreecell.event;

import java.util.EventObject;

/**
 * Game Event Class
 * 
 * @author Sateesh Gampala
 *
 */
public class GameEvent extends EventObject {

    private static final long serialVersionUID = -4830955110338193199L;
    
    private GameEvents event;
    private Object value;
    private int numeroJogadas;

    public GameEvent(Object source, GameEvents event, Object value, int numeroJogada) {
        super(source);
        this.event = event;
        this.value = value;
        this.numeroJogadas = numeroJogada;
    }

    public GameEvents getEvent() {
        return event;
    }
    
    public Object getValue() {
        return this.value;
    }
    
    public int getNumeroJogadas() {
        return this.numeroJogadas;
    }
}
