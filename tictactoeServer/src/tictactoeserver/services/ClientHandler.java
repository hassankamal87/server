/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tictactoeserver.services;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import tictactoeserver.model.Player;
import utility.JsonObjectHelper;

/**
 *
 * @author Mohamed Adel
 */
class ClientHandler extends Thread {

    DataInputStream dis;
    PrintStream ps;
    BufferedReader br;
    static Vector<ClientHandler> clientVector = new Vector<ClientHandler>();
    JSONObject clientJson = new JSONObject();
    JSONObject responseJson = new JSONObject();
    DataAccessLayer connection;
    String email;
    String opponentEmail;

    public ClientHandler(Socket s) {
        try {
            dis = new DataInputStream(s.getInputStream());
            ps = new PrintStream(s.getOutputStream());
            br = new BufferedReader(new InputStreamReader(dis));
            connection = DataAccessLayer.getInstance();
            clientVector.add(this);
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {

        while (true) {
            try {
                clientJson = readMessage();
            } catch (NullPointerException ex) {
                break;
            }

            String header = (String) clientJson.get(JsonObjectHelper.HEADER);
            switch (header) {
                case JsonObjectHelper.SIGNUP:
                    //signup server logic
                    signupLogic();
                    break;
                case JsonObjectHelper.LOGIN:
                    //login server logic
                    loginLogic();
                    break;
                case JsonObjectHelper.SEND_INVITATION:
                    //send invitaion logic
                    sendInvitaion();
                    break;
            }
        }
    }

    private JSONObject readMessage() throws NullPointerException {
        JSONObject clientJson = new JSONObject();
        try {
            clientJson = (JSONObject) new JSONParser().parse(br.readLine());

        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return clientJson;
    }

    private void signupLogic() {
        {
            try {
                boolean isExist;
                Player newPlayer = new Player(clientJson.get(JsonObjectHelper.NAME).toString(),
                        clientJson.get(JsonObjectHelper.EMAIL).toString(),
                        clientJson.get(JsonObjectHelper.PASSWORD).toString()
                );
                isExist = connection.checkPlayerExist(newPlayer.getEmail());
                if (!isExist) {
                    int result = connection.insert(newPlayer);
                    if (result > 0) {
                        //signedup success
                        responseJson.put(JsonObjectHelper.SIGNUP_STATUS, JsonObjectHelper.SIGNUP_SUCCESS);
                    }
                } else {
                    //send duplicate email using ps
                    responseJson.put(JsonObjectHelper.SIGNUP_STATUS, JsonObjectHelper.SIGNUP_FAIL_DUPLICATE);
                }
                ps.println(responseJson);

            } catch (SQLException ex) {
            }
        }
    }

    private void loginLogic() {
        ArrayList<Player> onlinePlayersList = new ArrayList<>();
        try {

            Player player = DataAccessLayer.getInstance().getPlayerByEmail(clientJson.get(JsonObjectHelper.EMAIL).toString());
            if (player != null) {
                if (player.getPassword().toString().equals(clientJson.get(JsonObjectHelper.PASSWORD).toString())) {
                    DataAccessLayer.getInstance().changeActiveStatus(player);
                    responseJson.put(JsonObjectHelper.SIGNIN_STATUS, JsonObjectHelper.SIGNIN_SUCCESS);
                    email = player.getEmail();
                } else {
                    responseJson.put(JsonObjectHelper.SIGNIN_STATUS, JsonObjectHelper.SIGNIN_FAIL);
                }
            } else {
                responseJson.put(JsonObjectHelper.SIGNIN_STATUS, JsonObjectHelper.SIGNIN_FAIL);
            }
            ps.println(responseJson);

            if (player != null) {
                onlinePlayersList = DataAccessLayer.getInstance().getAllPlayers();
                if (onlinePlayersList.size() > 0) {
                    for (Player p : onlinePlayersList) {
                        if (player.getEmail() != clientJson.get(JsonObjectHelper.EMAIL)) {
                            JSONObject playerJson = new JSONObject();
                            playerJson.put(JsonObjectHelper.HEADER, JsonObjectHelper.ONLINE_LIST);
                            playerJson.put(JsonObjectHelper.EMAIL, player.getEmail());
                            playerJson.put(JsonObjectHelper.NAME, player.getName());
                            ps.println(playerJson);
                        }

                    }
                    ps.println(new JSONObject().put(JsonObjectHelper.HEADER, JsonObjectHelper.END));
                }

            }
            //System.out.println(responseJson.toJSONString());

        } catch (SQLException ex) {
        }
    }
    
    private void sendInvitaion(){
        for(ClientHandler client : clientVector){
            if(client.email == opponentEmail){
                JSONObject invitationObject = new JSONObject();
                invitationObject.put(JsonObjectHelper.SENDER , email);
                invitationObject.put(JsonObjectHelper.SENDER , email);
                client.ps.println(invitationObject);
            }
        }
    }

}
