# 📧 UserFlow Email Sender - Microsserviço com Observabilidade

## 1. O que é o projeto?

Este projeto é um **microsserviço de cadastro de usuário e processamento e envio de e-mails (simulado)**, construído com foco em:

- Arquitetura orientada a eventos
- Resiliência
- Observabilidade real
- Métricas de negócio e técnicas
- Conteinerização
- Stack com microsserviços

Ele recebe eventos de e-mail, persiste no banco, processa, tenta enviar (simulado) e registra métricas detalhadas de:

- Processamento
- Envio SMTP
- Sucesso
- Falha
- DLQ
- Latência com percentis


---

## 2. Proposta de estudo

O projeto foi estruturado como **laboratório de estudo de microsserviços com observabilidade aplicada na prática**.

Objetivos:

- Separação clara de responsabilidades
- Instrumentação manual com Micrometer
- Métricas de negócio vs métricas técnicas
- Uso de Prometheus + Grafana
- Construção de dashboards significativos
- Simulação de falhas reais
- Estudo de retry e idempotência
- Containerização isolada por serviço

O intuito não foi apenas fazer rodar, mas também **entender o que está acontecendo**.

---

## 3. Stack utilizada

### Backend
- Java 17+
- Spring Boot
- Spring Data JPA
- Micrometer
- Actuator

### Banco
- PostgreSQL

### Mensageria
- RabbitMQ

### Observabilidade
- Prometheus
- Grafana

### Containerização
- Docker
- Docker Compose

---

## 4. Métricas implementadas

### 🔹 Processamento do Evento

**Counters**
- `email.process.success`
- `email.process.attempt.failure`
- `email.process.final.failure`

**Timer com status**
- `email.process.duration{status="success"}`
- `email.process.duration{status="failure"}`

Com:
- Histograma habilitado
- Percentis: p50, p95, p99

---

### 🔹 Envio SMTP

**Timer com status**
- `email.send.duration{status="success"}`
- `email.send.duration{status="failure"}`

Também com:
- Histograma
- Percentis

---

## 5. Gráficos no Grafana

O dashboard contém:

### Requests per minute
```promql
rate(email_process_duration_seconds_count[5m]) * 60
```

---

### Processing Error Rate (%)
```promql
rate(email_process_final_failure_total[5m]) / 
(rate(email_process_success_total[5m]) + 
rate(email_process_final_failure_total[5m])) 
* 100
```

---

### Process Success vs Failure
```promql
sum(rate(email_process_duration_seconds_count[5m])) by (status)
```

---

###  Processing Latency (p95)
```promql
histogram_quantile(0.95,
  sum(rate(email_process_duration_seconds_bucket[5m])) by (le)
)
```

---

### Processing Latency (p99)
```promql
histogram_quantile(0.99, 
sum(rate(email_process_duration_seconds_bucket[5m])) 
by (le))
```

---

### DLQ Counter
```promql
increase(email_process_final_failure_total[15m])
```

---

## 6. Classe responsável pelo processamento

A classe principal de orquestração é:

```
EmailService
```

Responsável por:

- Persistência do evento
- Controle de status (PENDING → PROCESSING → SENT/FAILED)
- Retry
- Incremento de métricas
- Registro de latência via `Timer.Sample`
- Simulação de falha SMTP

### Ponto importante

O envio SMTP é medido separadamente do processamento geral, permitindo:

- Medir tempo total do evento
- Medir apenas o tempo do envio externo

Isso separa latência interna de latência de integração.

---

## 7. Conteinerização

Cada componente roda isoladamente:

- email-service
- postgres
- rabbitmq (quando usado)
- prometheus
- grafana

Tudo orquestrado via Docker Compose.

Isso permite:

- Ambiente reprodutível
- Subir stack completa com um comando
- Isolamento de responsabilidades
- Simular ambiente real de produção

---

## 8. Separação de arquivos YAML

O projeto utiliza separação por responsabilidade:

- `docker-compose.yml` → Orquestração principal
- `prometheus.yml` → Configuração de scrape
- `application.yml` → Configuração do serviço
- `grafana provisioning` → Dashboards e datasources

---

## 9. Observabilidade

- Métricas de negócio
- Métricas técnicas
- Percentis reais
- Histograma
- Error Rate real
- Métricas por status
- Separação entre falha tentativa e falha final

Isso permite:

- Criar SLOs
- Calcular Error Budget
- Monitorar Burn Rate
- Detectar regressão de performance

---

## 10. Simulação de falha

A falha é simulada quando o email do request contém:

```
"fail"
```

Isso permite:

- Gerar métricas de erro
- Validar dashboards
- Testar retry
- Validar DLQ

---

## 11. Retry

O método `retryEmail(UUID id)`:

- Só permite retry se status = FAILED
- Atualiza métricas de tentativa
- Reprocessa envio
- Mantém consistência de estado

---

## 12. Boas práticas aplicadas

✔ Idempotência via controle de duplicidade  
✔ Separação de métricas por domínio  
✔ Timer com label `status`  
✔ Percentis
✔ Containerização isolada

---

## 13. Para o proximo laboratório

- Implementar SLO formal
- Criar alertas no Prometheus
- Implementar OpenTelemetry (tracing)
- Adicionar métricas de fila (Rabbit)
- Adicionar dashboard de infraestrutura
- Adicionar logs estruturados

---

# 🔄 Fluxo do Evento

```
[Producer / API / Evento]
            ↓
        RabbitMQ
            ↓
      EmailService
            ↓
  ┌─────────────────────┐
  │ Persist (PENDING)   │
  └─────────────────────┘
            ↓
  ┌─────────────────────┐
  │ PROCESSING          │
  │ Timer.start()       │
  └─────────────────────┘
            ↓
        sendEmail()
            ↓
   ┌───────────────┬───────────────┐
   │               │               │
 SUCCESS        FAILURE         EXCEPTION
   │               │               │
   ↓               ↓               ↓
Timer success   Timer failure   Retry (Rabbit)
Counter++       Counter++       
   ↓               ↓
 Status = SENT   Status = FAILED
   ↓               ↓
Persist         Persist
```

### Estados possíveis

- `PENDING`
- `PROCESSING`
- `SENT`
- `FAILED`

### Observabilidade aplicada no fluxo

- Timer para processamento total
- Timer separado para envio SMTP
- Counter de sucesso
- Counter de falha tentativa
- Counter de falha final (DLQ)
- Percentis p95, p99

Separação clara entre:
- Latência interna
- Latência de integração externa

---

# 🏗 Diagrama de Arquitetura

```
                   ┌────────────────────┐
                   │   Producer / API   │
                   └─────────┬──────────┘
                             │
                             ▼
                      ┌──────────────┐
                      │  RabbitMQ    │
                      └──────┬───────┘
                             │
                             ▼
                   ┌───────────────────┐
                   │   Email Service   │
                   │-------------------│
                   │ Spring Boot       │
                   │ Micrometer        │
                   │ JPA               │
                   └──────┬────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼                               ▼
   ┌──────────────┐               ┌──────────────┐
   │ PostgreSQL   │               │  Prometheus  │
   └──────────────┘               └──────┬───────┘
                                         │
                                         ▼
                                   ┌────────────┐
                                   │  Grafana   │
                                   └────────────┘
```

### Responsabilidades

- **RabbitMQ** → transporte de eventos
- **Email Service** → processamento + métricas
- **Postgres** → persistência
- **Prometheus** → coleta de métricas
- **Grafana** → visualização

Tudo isolado via Docker.

---

# 🚀 Como Subir o Ambiente

Na raiz do projeto:

```bash
docker-compose up -d --build
```

---

## Serviços disponíveis

| Serviço       | URL |
|--------------|------|
| Email API     | http://localhost:8080 |
| Prometheus    | http://localhost:9090 |
| Grafana       | http://localhost:3000 |
| RabbitMQ UI   | http://localhost:15672 |

---

## Configurar Grafana

1. Acessar `localhost:3000`
2. Adicionar Prometheus como datasource:
   ```
   http://prometheus:9090
   ```
3. Importar o dashboard
4. Ajustar intervalo para 5m ou 15m

---

## Gerar requisições para testar métricas

Após executar containers e os serviços, execute a classe LoadGenerator que fará
vários requests. Acompanhe no grafana.

---