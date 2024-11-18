# Anotações

Projeto seguindo linha de clean architecture.


# Dependência envio de email Amazon SES

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>ses</artifactId>
    <version>2.29.12</version>
</dependency>
```

# Camadas

Teremos várias camadas responsáveis por determinadas coisas seguindo a arquitetura limpa.

A ideia é ir criando as camadas e aos poucos abstraindo tudo através de interfaces dividindo da forma correta.

Core - responsável por regra de negócio e caso de uso.

Application - responsável por implementar os casos da uso da camada core.

Adapters - A parte de gateway. Responsável por adaptar o mundo de fora para uma interface! Nossa camada de application,
irá depender dessa interface da camada de adapter.

## Camada core

Regras de negócio e casos de uso.

Caso de uso = o que a aplicação faz, mas não COMO ele faz.

Que no caso, é um envio de email! Recebendo um subject (assunto), to e um body (mensagem de fato).

Então criaremos um pacote core.

Dentro de core uma interface chamada ``EmailSenderUseCase``. Ela terá um método void responsável por enviar email.

Este método, irá receber um to, subject e body do email.

Essa interface, ela nada mais é do que um contrato que irá definir o comportamento da nossa aplicação.

Teremos também as outras camadas responsáveis por implementar isso (camada de aplicação/adapters). 

## Camada application

Aqui, implementaremos os casos de uso advindos da camada core (através da interface).

A camada application conhece a core. Mas a core, não conhece ninguém.

Por sua vez, a camada de application também não vai conhecer ninguém de fora (EmailServiceProvider - SES da amazon). Ela
irá conhecer só a interface.

Em application teremos nossa classe EmailSenderService (@Service), implementando a interface do core.

Essa camada irá conectar o core ao "mundo externo", como se fosse uma ponte.

A camada de application terá um EmailSenderGateway que será inserido dentro do construtor da classe de aplicação.

Esse EmailSenderGateway vem da camada de adapters.

## Camada adapters (interface adapters)

Será a interface EmailSenderGateway.

Essa camada vai adaptar o mundo de fora para uma interface e a nossa aplicação irá depender dessa interface (e não do
mundo de fora).

Explicando: para o nosso EmailSenderService não depender diretamente do AWS SDK ou do SendGrid, ele vai depender de 
uma interface.

Essa interface é uma interface adapter (um adaptador de interface, que nós criamos).

Depois quando criarmos a classe do AWS que fará o envio de email e vai usar o AWS SDK, essa classe vai implementar essa
interface adapter (EmailSenderGateWay).

Portanto, nós não dependeremos da AWS! A AWS irá implementar da interface que definimos e o nosso Service irá depender
dela também.

Essa interface irá possuir o mesmo método do nosso core (um método void para enviar email com (to, subject e body)).

Porém, tem uma diferença!

O método que está na interface do core, representa a lógica de negócio da nossa aplicação (alto nível).

O método dessa interface do adapter é nada mais do que um contrato para que a nossa aplicação interaja com os serviços
de fora (os e-mails services providers: SendGrid ou AWS SES).

Então mesmo o corpo dos métodos sendo iguais, elas não representam a mesma coisa.

UseCase = casos de uso da aplicação, regra de negócio.

SenderGateway = o contrato da aplicação com o provedor de serviço de envio de email. 

Com isso, nós faremos o seguinte.

Na classe de EmailSenderService, dentro do método que implementa a interface UseCase, daremos um ``this.emailSenderGateway``,
passando o to, subject e body.

Ou seja, dentro do método da camada core, usaremos a injeção da camada de adapter.

## Camada infraestrutura

Pode ser chamada de infra no pacote.

Responsável por interagir com os serviços (estruturas externas), onde implementaremos a comunicação com o AWS SES.

Dentro do pacote infra, teremos outro pacote chamado "ses", onde criaremos uma classe denominada SesEmailSender.

Essa classe será @Service.

Nós criamos esse pacote dentro do pacote infra porque o SES pode não ser o único serviço externo que a aplicação vai 
utilizar. Ela pode usar a API do Google, Facebook, Instagram... ou até mesmo outros microservices.

Então é uma boa ter um pacote para cada serviço externo utilizado.

A classe que ficará dentro do pacote SES irá implementar a interface da camada de adapters (SenderGateway), que é a
interface que nosso EmailService depende.

Essa classe do pacote SES, além disso, terá a dependência do Amazon SDK (AmazonSimpleEmailService), onde será injetada
por meio de um construtor com @Autowired.

A partir da injeção, poderemos começar a construir o método de sendEmail (que veio da interface Gateway).

### Montando request que será enviada para AWS

Criaremos uma variável do tipo SendEmailRequest (que vem da biblioteca da AWS) com um new().

A partir disso, passaremos os parâmetros com ".".

O primeiro é ``withSource()``, ou seja, quem está enviando (qual a origem do email). Colocaremos o email que iremos
eventualmente cadastrar na AWS.

O segundo é ``withDestination()``, quem vai receber o email, daremos um ``new Destination().withToAddress(to)``. Ou seja,
quem veio como parâmetro do método da interface.

E por fim um ``withMessage()``, com um ``new Message()``. Dentro do new Message passaremos os construtores:

``.withSubject()``, passando um ``new Content(subject)`` -> subject do parâmetro também.

E um ``withBody()``, passando um ``new Body().withText(new Content(body))`` -> body do parâmetro.

Com o método pronto, faremos um try-catch (ainda dentro do sendEmail).

#### Try

Faremos uma referência ao amazonSimpleEmailService com o this, passando o request criado acima.

#### Catch

Caso dê algum erro (AmazonServiceException), pegaremos o erro e lançaremos uma ``new EmailServiceException`` (que iremos
criar).

## EmailServiceException

Essa exceção ou qualquer outra exceção personalizada da nossa aplicação, fazem parte ao core. Pois são relacionadas
a nossa lógica e regra de negócio.

Portanto, dentro da camada core criamos um novo pacote "exceptions". E teremos a nossa exception EmailServiceException.



# Controllers

Criamos o nosso pacote controllers, bem como a classe ``EmailSenderController``, passando as anotações.

Endpoint será: "/api/email".

As dependências serão: o EmailSenderService.

Ou seja:

1. O ponto de entrada é o Controller;
2. O Controller, depende do EmailSenderService;
3. O EmailSenderService, implementa o EmailSenderUseCase (da camada Core);
4. O EmailSenderUseCase depende do EmailSenderGateway (camada adapter);
5. A nossa classe da AWS, SesEmailSender, implementa a EmailSenderGateway que por sua vez, é injetada no EmailSenderService,
que é utilizado pelo Controller.

## PostMapping

Nosso método será bem simples. Retornará uma ResponseEntity do tipo String. Vai ter um RequestBody de EmailRequest.

Dentro do método terá um try-catch. No try, fará a referência com this ao EmailSenderService, no método sendEmail,
passando como parâmetro o request (to, subject, body).

No catch o EmailServiceException. Será uma ResponseEntity com HttpStatus (internal server error) passando uma mensagem.

### EmailRequestDTO (Record)

Vai ficar dentro do core da aplicação, porque está relacionado a regra de negócio (o que devemos receber como informação).

Só passar como parâmetro o "to, subject e body".

# Configurando SDK AWS SES

Faremos primeiro a configuração dentro da aplicação e depois no console da Amazon.

## Aplicação

Colocar chaves de acesso no application.properties

```properties
aws:
acessKeyId:
secretKey:
region:
```

## AWS SeS Config

Essa classe fará o Spring ser responsável por fazer a instanciação correta da classe da Amazon (nosso client AWS SeS).

Esse client no caso seria o AmazonSimpleEmailService presente no EmailSender.

Então a gente precisa mostrar para o Spring como criar uma instância correta dessa dependência.

Essa classe terá um método simples.

O método terá um retorno de AmazonSimpleEmailService. Dentro dele, daremos um return no ServiceClientBuilder, com um
``.standart`` e ``.build``.

A partir disso, quando o nosso SesEmailSender falar para o Spring que ele precisa daquela classe que é do tipo
AmazonSimpleEmailService e o Spring precisa injetar automaticamente, ele vai procurar onde existe uma classe desse tipo
(procurando uma Config), então ele chama essa classe Config que criamos.




## Configurando SES no site Amazon

### SES

Pesquisa por SES.

Vá até identidades verificadas ⇨ criar identidade ⇨ selecione tipo de identidade: endereço de email ⇨ coloque o endereço
de email ⇨ crie identidade.

A AWS vai disparar um email para o que colocamos e iremos verificar.

### IAM

Pesquisa por IAM.

Aqui iremos gerenciar contas (usuário/autenticação).

Clique em Usuários e criaremos um novo para representar a nossa aplicação. 

Coloque o nome ⇨ próximo ⇨ marque "anexar política diretamente" ⇨ coloque AmazonSESFullAccess ⇨ crie usuário.

Será a única permissão que ele terá.

#### Chave de acesso

No usuário criado vá em ⇨ criar chave de acesso ⇨ caso de uso será: aplicação em execução em um serviço computacional
AWS ⇨ etiqueta de descrição pode ser "aws-sdk-java".

Copie a chave de acesso e secretKey e coloca no application.properties.

Como não dá para acessar essas chaves depois, é uma boa baixar o arquivo ".csv".



## Testando

Inicie a aplicação e abra o postman.

Crie um request Postman e passa o endereço.

Coloque o body do tipo JSON

```json
{
  "to": "olavomoreiracontato@gmail.com",
  "subject": "teste",
  "body": "hello from teste"
}
```

Como estamos na parte de testes da AWS, só iremos conseguir disparar e-mails para as identidades verificadas. Caso queria
enviar para outro email, é só verificar no AWS aceitando a requisição.