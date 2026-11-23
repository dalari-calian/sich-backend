package com.sich.common.base;

/**
 * Dados pessoais e endereço que cliente e prestador têm em comum, para o perfil ser
 * lido e editado do mesmo jeito nos dois. Os métodos são os gerados pelo Lombok nas
 * entidades.
 */
public interface PersonalData {

    String getName();

    void setName(String name);

    String getPhone();

    void setPhone(String phone);

    String getCnpjCpf();

    void setCnpjCpf(String cnpjCpf);

    String getCity();

    void setCity(String city);

    String getState();

    void setState(String state);

    String getStreet();

    void setStreet(String street);

    String getComplementAdress();

    void setComplementAdress(String complementAdress);

    String getNeighborhood();

    void setNeighborhood(String neighborhood);

    String getCep();

    void setCep(String cep);

    String getNumberAdress();

    void setNumberAdress(String numberAdress);
}
