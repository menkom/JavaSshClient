import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Client to execute Linux commands via ssh using Java
 */
public class SshClient {

    public static List<String> execute(String command, Authorization authorization) {
        Session session = null;
        ChannelExec channel = null;
        try {
            session = initSession(authorization);
            session.connect();
            // Opens a new channel of some type over this connection
            // * shell - ChannelShell
            // * exec - ChannelExec
            // * direct-tcpip - ChannelDirectTCPIP
            // * sftp - ChannelSftp
            // * subsystem - ChannelSubsystem
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            InputStream inputStream = channel.getInputStream();
            channel.connect();

            String result = toString(inputStream);
            return Arrays.asList(result.split("\n"));
        } catch (JSchException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeConnection(channel, session);
        }
    }

    private static String toString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }

    private static Session initSession(Authorization authorization) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(authorization.getLogin(), authorization.getHost());
        session.setPassword(authorization.getPassword());
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
        //If you use RSA key then you have to point on it.
//        jsch.addIdentity("~/.ssh/id_rsa");
        // yes, JSch will never automatically add host keys to the $HOME/.ssh/known_hosts file, and refuses to connect to hosts whose host key has changed. This property forces the user to manually add all new hosts.
        // no, JSch will automatically add new host keys to the user known hosts files
        // ask, new host keys will be added to the user known host files only after the user has confirmed that
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }

    private static void closeConnection(ChannelExec channel, Session session) {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Authorization {

        String host;

        String login;

        String password;
    }
}
