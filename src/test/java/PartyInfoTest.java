import org.example.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PartyInfoTest {
    private List<PartyInfo> infoList;
    private List<UserInfo> userList;

    @BeforeEach
    public void init() {
        initializeUserList();
        initializeParties();
    }


    @Test
    void testAddHostToParty(){
        for (PartyInfo info : infoList) {
            boolean cookingParty = info.getType().getName().equals("Cooking");
            String hostid = info.getHostInfo().getId();
            UserInfo hostInfo = userList.stream().filter(userInfo -> userInfo.getId().equals(hostid)).findFirst().orElse(null);
            if(hostInfo != null) {
                addUserToParty(info, hostInfo);
                assertTrue(info.getUserList().stream().noneMatch(userInfo -> userInfo.getId().equals(hostid)) || cookingParty);
            }
        }
    }

    @Test
    void testAddOneUserToParty(){
        for (PartyInfo info : infoList) {
            boolean cookingParty = info.getType().getName().equals("Cooking");
            String hostid = info.getHostInfo().getId();
            UserInfo userToAdd = userList.stream().filter(userInfo -> !userInfo.getId().equals(hostid)).findFirst().orElse(null);
            if(userToAdd == null) continue;

            addUserToParty(info, userToAdd);
            assertTrue(info.getUserList().stream().anyMatch(userInfo -> userInfo.getId().equals(userToAdd.getId())) || cookingParty);
        }
    }

    @Test
    void testAddUsersToParty(){
        for (PartyInfo info : infoList) {
            boolean cookingParty = info.getType().getName().equals("Cooking");
            String hostid = info.getHostInfo().getId();
            UserInfo userToAdd = userList.stream().filter(userInfo -> !userInfo.getId().equals(hostid)).findFirst().orElse(null);
            if(userToAdd != null){
                info.addUser(userToAdd);
                assertTrue(info.getUserList().stream().anyMatch(userInfo -> userInfo.getId().equals(userToAdd.getId())) || cookingParty);
            }
        }
    }

    @Test
    void testStatus(){
        for (PartyInfo info : infoList) {
            info.setStatus(new Random().nextBoolean());
            assertTrue(info.isStatus() ? PartyInfo.getStatus(info).equals("Open") : PartyInfo.getStatus(info).equals("Closed"));
        }
    }

    public void addUserToParty(PartyInfo info, UserInfo user){
        if(info.getRecipe() != null) {
            List<RecipeRole> roles = info.getRecipe().getRoles();
            user.setRecipeRole(new Random().nextBoolean() ? Common.getRandomItemFromList(roles).getRoleName() : "");
        }

        info.addUser(user);
    }

    public void initializeParties(){
        Recipes.loadRecipesFromFile();
        List<Recipe> recipeList = Recipes.getRecipeList();
        List<TypeInfo> typeList = TypeInfo.getOrCreateTypeCodes();
        List<String> servers = PartyInfo.SERVERS;

        infoList = new ArrayList<>();
        Random random = new Random();
        int numberOfParties = random.nextInt(10, 25);

        for(int i = 0; i < numberOfParties; i++) {
            PartyInfo info = new PartyInfo();
            info.setType(typeList.get(random.nextInt(typeList.size())));
            info.setHostInfo(userList.get(random.nextInt(userList.size())));
            info.setPeople(random.nextInt(5, 20));
            info.setServer(servers.get(random.nextInt(servers.size())));
            info.setStatus(random.nextBoolean());
            info.setCommandGuid(Common.createGUID());
            info.setTimestamp("");
            info.setQuantity(random.nextInt(5, 20));
            info.setCreated(LocalDateTime.now());
            info.setVoice(random.nextBoolean());
            info.setMessageid(0);
            info.setChannelid(0);

            info.setRecipe(random.nextBoolean() ? recipeList.get(random.nextInt(recipeList.size())) : null);
            info.setUserList(new ArrayList<>());

            infoList.add(info);
        }
    }

    public void initializeUserList(){
        userList = new ArrayList<>();
        Random random = new Random();
        int numberOfUsers = random.nextInt(20, 50);

        for(int i = 0; i < numberOfUsers; i++) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId(Common.createGUID());
            userInfo.setName(Common.createRandomString(1, 22));
            userInfo.setRecipeRole("");
            userList.add(userInfo);
        }
    }

}
