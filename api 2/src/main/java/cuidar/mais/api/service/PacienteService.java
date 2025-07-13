package cuidar.mais.api.service;

import cuidar.mais.api.dto.PacienteDTO;
import cuidar.mais.api.models.Agendamento;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Paciente;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.HorarioDisponivelRepository;
import cuidar.mais.api.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PacienteService {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private HorarioDisponivelRepository horarioDisponivelRepository;

    @Autowired
    private HorarioDisponivelService horarioDisponivelService;

    /**
     * Busca todos os pacientes de um psicólogo
     */
    public List<PacienteDTO> buscarPorPsicologo(Long psicologoId) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        return pacienteRepository.findByPsicologo(psicologo).stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca um paciente pelo ID
     */
    public PacienteDTO buscarPorId(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        return converterParaDTO(paciente);
    }

    /**
     * Cria um novo paciente com agendamentos recorrentes
     */
    @Transactional
    public PacienteDTO criar(PacienteDTO dto) {
        Usuario psicologo = usuarioService.buscarPorId(dto.getPsicologoId());

        // Cria o paciente
        Paciente paciente = new Paciente();
        paciente.setNome(dto.getNome());
        paciente.setSobrenome(dto.getSobrenome());
        paciente.setSexo(dto.getSexo());
        paciente.setDataNascimento(dto.getDataNascimento());
        paciente.setEmail(dto.getEmail());
        paciente.setTelefone(dto.getTelefone());

        // Define a imagem apenas se for fornecida
        if (dto.getImagemBase64() != null && !dto.getImagemBase64().isEmpty()) {
            paciente.setImagemBase64(dto.getImagemBase64());
            paciente.setImagemTipo(dto.getImagemTipo());
        }
        paciente.setPsicologo(psicologo);
        paciente.setTotalSessoes(dto.getTotalSessoes());
        paciente.setAtivo(true);

        // Salva o paciente
        paciente = pacienteRepository.save(paciente);

        // Usa o ID do paciente para buscar o usuário correspondente
        Long pacienteId = paciente.getId();

        // Busca o usuário pelo ID do paciente
        Usuario pacienteUsuario = null;

        // Tenta encontrar um usuário com o ID igual ao do paciente
        try {
            pacienteUsuario = usuarioService.buscarPorId(pacienteId);
            System.out.println("Encontrou usuário com ID " + pacienteId);
        } catch (RuntimeException e) {
            System.out.println("Não encontrou usuário com ID " + pacienteId);
        }

        // Se não encontrou, tenta encontrar um usuário com email contendo o ID do paciente
        if (pacienteUsuario == null) {
            List<Usuario> todosUsuarios = usuarioService.listarTodos();
            for (Usuario u : todosUsuarios) {
                if (u.getEmail() != null && u.getEmail().contains("." + pacienteId + "@")) {
                    pacienteUsuario = u;
                    System.out.println("Encontrou usuário com email contendo ID " + pacienteId);
                    break;
                }
            }
        }

        // Se ainda não encontrou, cria um novo usuário (sem senha, pois pacientes não terão acesso ao sistema)
        if (pacienteUsuario == null) {
            final String nomeEmail = paciente.getNome().toLowerCase();
            final String sobrenomeEmail = paciente.getSobrenome().toLowerCase();
            final String nomeCompleto = paciente.getNome() + " " + paciente.getSobrenome();

            // Cria um email único baseado no nome e ID do paciente
            String email = paciente.getEmail();

            pacienteUsuario = new Usuario();
            pacienteUsuario.setNome(nomeCompleto);
            pacienteUsuario.setEmail(email);
            // Não definimos senha, pois pacientes não terão acesso ao sistema
            pacienteUsuario.setAtivo(true);
            pacienteUsuario.setDataCriacao(LocalDateTime.now());
            pacienteUsuario.setPerfis(new java.util.ArrayList<>());
            pacienteUsuario = usuarioService.salvar(pacienteUsuario);

            System.out.println("Criou novo usuário com ID " + pacienteUsuario.getId() + " para paciente com ID " + pacienteId);
        }

        // Busca o horário disponível selecionado
        HorarioDisponivel horarioDisponivel = horarioDisponivelRepository.findById(dto.getHorarioDisponivelId())
                .orElseThrow(() -> new RuntimeException("Horário disponível não encontrado"));

        // Desativa o horário disponível (seta o campo ativo para false)
        horarioDisponivel = horarioDisponivelService.desativarHorario(horarioDisponivel.getId());

        // Gera um ID único para a série de agendamentos
        UUID serieId = UUID.randomUUID();

        // Cria os agendamentos recorrentes
        LocalDate dataAgendamento = dto.getDataInicio();
        for (int i = 0; i < dto.getTotalSessoes(); i++) {
            // Encontra a próxima data com o dia da semana correto
            while (dataAgendamento.getDayOfWeek() != dto.getDiaSemana()) {
                dataAgendamento = dataAgendamento.plusDays(1);
            }

            // Cria o agendamento
            Agendamento agendamento = new Agendamento();
            agendamento.setPsicologo(psicologo);
            // Usar o Usuario do paciente, não o psicólogo
            agendamento.setPaciente(pacienteUsuario);
            agendamento.setSerieId(serieId);
            agendamento.setNumeroSessao(i + 1);
            agendamento.setData(dataAgendamento);
            agendamento.setHoraInicio(horarioDisponivel.getHoraInicio());
            agendamento.setHoraFim(horarioDisponivel.getHoraFim());
            agendamento.setStatus(Agendamento.StatusAgendamento.AGENDADO);

            // Salva o agendamento
            agendamentoService.criar(agendamento);

            // Avança para a próxima semana
            dataAgendamento = dataAgendamento.plusDays(7);
        }

        return converterParaDTO(paciente);
    }

    /**
     * Atualiza um paciente existente
     */
    @Transactional
    public PacienteDTO atualizar(Long id, PacienteDTO dto) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Guarda o valor antigo de totalSessoes para verificar se houve alteração
        Integer totalSessoesAntigo = paciente.getTotalSessoes();

        // Atualiza os campos
        paciente.setNome(dto.getNome());
        paciente.setSobrenome(dto.getSobrenome());
        paciente.setSexo(dto.getSexo());
        paciente.setDataNascimento(dto.getDataNascimento());
        paciente.setEmail(dto.getEmail());
        paciente.setTelefone(dto.getTelefone());

        // Atualiza a imagem apenas se for fornecida
        if (dto.getImagemBase64() != null && !dto.getImagemBase64().isEmpty()) {
            paciente.setImagemBase64(dto.getImagemBase64());
            paciente.setImagemTipo(dto.getImagemTipo());
        }

        paciente.setAtivo(dto.getAtivo());

        // Atualiza o total de sessões se fornecido
        boolean totalSessoesAlterado = false;
        if (dto.getTotalSessoes() != null) {
            // Verifica se o valor foi alterado
            totalSessoesAlterado = !dto.getTotalSessoes().equals(totalSessoesAntigo);
            paciente.setTotalSessoes(dto.getTotalSessoes());
        }

        // Salva o paciente
        paciente = pacienteRepository.save(paciente);

        // Verifica se houve alteração no dia ou horário
        if (dto.getDiaSemana() != null && dto.getHorarioDisponivelId() != null && 
            dto.getDiaSemanaAntigo() != null && dto.getHorarioDisponivelIdAntigo() != null && 
            dto.getSerieId() != null &&
            (!dto.getDiaSemana().equals(dto.getDiaSemanaAntigo()) || 
             !dto.getHorarioDisponivelId().equals(dto.getHorarioDisponivelIdAntigo()))) {

            // Busca o horário disponível selecionado
            HorarioDisponivel novoHorarioDisponivel = horarioDisponivelRepository.findById(dto.getHorarioDisponivelId())
                    .orElseThrow(() -> new RuntimeException("Novo horário disponível não encontrado"));

            // Verifica se o horário está ativo
            if (!novoHorarioDisponivel.getAtivo()) {
                throw new RuntimeException("O horário selecionado não está disponível");
            }

            // Busca o horário disponível antigo
            HorarioDisponivel horarioDisponivelAntigo = horarioDisponivelRepository.findById(dto.getHorarioDisponivelIdAntigo())
                    .orElseThrow(() -> new RuntimeException("Horário disponível antigo não encontrado"));

            // Desativa o novo horário disponível
            horarioDisponivelService.desativarHorario(novoHorarioDisponivel.getId());

            // Reativa o horário disponível antigo
            horarioDisponivelService.reativarHorario(horarioDisponivelAntigo.getId());

            // Atualiza os agendamentos
            agendamentoService.atualizarDiaEHorarioDaSerie(
                dto.getSerieId(), 
                dto.getDiaSemana(), 
                novoHorarioDisponivel.getHoraInicio(), 
                novoHorarioDisponivel.getHoraFim()
            );
        }

        // Se o total de sessões foi alterado, cria automaticamente as sessões adicionais
        if (totalSessoesAlterado && paciente.getAtivo()) {
            try {
                System.out.println("Total de sessões alterado de " + totalSessoesAntigo + " para " + paciente.getTotalSessoes() + ". Criando sessões adicionais automaticamente.");
                criarSessoesAdicionais(paciente.getId());
            } catch (Exception e) {
                System.err.println("Erro ao criar sessões adicionais automaticamente: " + e.getMessage());
                // Não lançamos a exceção para não interromper o fluxo de atualização do paciente
            }
        }

        return converterParaDTO(paciente);
    }

    /**
     * Busca o horário atual de um paciente
     * Este método retorna o horário disponível associado ao agendamento mais recente do paciente
     */
    public java.util.Map<String, Object> buscarHorarioAtual(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        java.util.Map<String, Object> resultado = new java.util.HashMap<>();

        try {
            // Busca o usuário do paciente
            Usuario pacienteUsuario = null;
            try {
                pacienteUsuario = usuarioService.buscarPorId(paciente.getId());
            } catch (Exception e) {
                // Tenta encontrar o usuário pelo email
                List<Usuario> usuarios = usuarioService.listarTodos();
                for (Usuario u : usuarios) {
                    if (u.getEmail() != null && u.getEmail().contains("." + paciente.getId() + "@")) {
                        pacienteUsuario = u;
                        break;
                    }
                }
            }

            if (pacienteUsuario == null) {
                throw new RuntimeException("Usuário do paciente não encontrado");
            }

            // Busca os agendamentos ativos e agendados do paciente
            List<Agendamento> agendamentos = agendamentoService.buscarAtivosEAgendadosPorPaciente(pacienteUsuario.getId());
            if (agendamentos.isEmpty()) {
                throw new RuntimeException("Paciente não possui agendamentos ativos");
            }

            // Pega o primeiro agendamento (mais recente)
            Agendamento agendamento = agendamentos.get(0);

            // Busca o horário disponível correspondente
            List<HorarioDisponivel> horarios = horarioDisponivelRepository.findByPsicologoAndDiaSemana(
                    paciente.getPsicologo(), agendamento.getData().getDayOfWeek());

            HorarioDisponivel horarioEncontrado = null;
            for (HorarioDisponivel horario : horarios) {
                if (horario.getHoraInicio().equals(agendamento.getHoraInicio()) && 
                    horario.getHoraFim().equals(agendamento.getHoraFim())) {
                    horarioEncontrado = horario;
                    break;
                }
            }

            if (horarioEncontrado == null) {
                // Se não encontrou um horário exato, cria um objeto com as informações do agendamento
                resultado.put("id", -1); // ID fictício
                resultado.put("diaSemana", agendamento.getData().getDayOfWeek().toString());
                resultado.put("horaInicio", agendamento.getHoraInicio().toString());
                resultado.put("horaFim", agendamento.getHoraFim().toString());
                resultado.put("horario", agendamento.getHoraInicio().toString().substring(0, 5) + " - " + 
                                        agendamento.getHoraFim().toString().substring(0, 5));
            } else {
                // Se encontrou o horário, retorna suas informações
                resultado.put("id", horarioEncontrado.getId());
                resultado.put("diaSemana", horarioEncontrado.getDiaSemana().toString());
                resultado.put("horaInicio", horarioEncontrado.getHoraInicio().toString());
                resultado.put("horaFim", horarioEncontrado.getHoraFim().toString());
                resultado.put("horario", horarioEncontrado.getHoraInicio().toString().substring(0, 5) + " - " + 
                                        horarioEncontrado.getHoraFim().toString().substring(0, 5));
                resultado.put("disponivel", horarioEncontrado.getAtivo());
            }

            // Adiciona informações do agendamento
            resultado.put("agendamentoId", agendamento.getId());
            resultado.put("serieId", agendamento.getSerieId().toString());
            resultado.put("data", agendamento.getData().toString());

            return resultado;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar horário atual: " + e.getMessage(), e);
        }
    }

    /**
     * Inativa um paciente e todos os seus agendamentos ativos
     * Também reativa o horário que estava vinculado aos agendamentos
     * @param pacienteId ID do paciente a ser inativado
     * @return O paciente inativado
     */
    @Transactional
    public PacienteDTO inativarPaciente(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Busca o usuário do paciente
        Usuario pacienteUsuario = null;
        try {
            pacienteUsuario = usuarioService.buscarPorId(paciente.getId());
        } catch (Exception e) {
            // Tenta encontrar o usuário pelo email
            List<Usuario> usuarios = usuarioService.listarTodos();
            for (Usuario u : usuarios) {
                if (u.getEmail() != null && u.getEmail().contains("." + paciente.getId() + "@")) {
                    pacienteUsuario = u;
                    break;
                }
            }
        }

        if (pacienteUsuario == null) {
            throw new RuntimeException("Usuário do paciente não encontrado");
        }

        // Inativa o paciente
        paciente.setAtivo(false);
        paciente = pacienteRepository.save(paciente);

        // Busca todos os agendamentos ativos do paciente
        List<Agendamento> agendamentos = agendamentoService.buscarAtivosPorPaciente(pacienteUsuario.getId());

        // Para cada agendamento, inativa e reativa o horário correspondente
        for (Agendamento agendamento : agendamentos) {
            // Inativa o agendamento
            agendamentoService.atualizarStatus(agendamento.getId(), Agendamento.StatusAgendamento.CANCELADO);

            // Busca o horário disponível correspondente
            List<HorarioDisponivel> horarios = horarioDisponivelRepository.findByPsicologoAndDiaSemana(
                    paciente.getPsicologo(), agendamento.getData().getDayOfWeek());

            for (HorarioDisponivel horario : horarios) {
                if (horario.getHoraInicio().equals(agendamento.getHoraInicio()) && 
                    horario.getHoraFim().equals(agendamento.getHoraFim())) {
                    // Reativa o horário
                    horarioDisponivelService.reativarHorario(horario.getId());
                    break;
                }
            }
        }

        return converterParaDTO(paciente);
    }

    /**
     * Reativa um paciente que estava inativo
     * @param pacienteId ID do paciente a ser reativado
     * @return O paciente reativado
     */
    @Transactional
    public PacienteDTO reativarPaciente(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Verifica se o paciente já está ativo
        if (paciente.getAtivo()) {
            return converterParaDTO(paciente);
        }

        // Reativa o paciente
        paciente.setAtivo(true);
        paciente = pacienteRepository.save(paciente);

        System.out.println("Paciente reativado com sucesso: " + paciente.getId());

        return converterParaDTO(paciente);
    }

    /**
     * Cria sessões adicionais para um paciente
     * Este método cria novas sessões baseadas no total de sessões configurado para o paciente
     * @param pacienteId ID do paciente
     * @return O paciente com as sessões adicionais
     */
    @Transactional
    public PacienteDTO criarSessoesAdicionais(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Verifica se o paciente está ativo
        if (!paciente.getAtivo()) {
            throw new RuntimeException("Não é possível criar sessões adicionais para um paciente inativo");
        }

        // Busca o usuário do paciente
        Usuario pacienteUsuario = null;
        try {
            pacienteUsuario = usuarioService.buscarPorId(paciente.getId());
        } catch (Exception e) {
            // Tenta encontrar o usuário pelo email
            List<Usuario> usuarios = usuarioService.listarTodos();
            for (Usuario u : usuarios) {
                if (u.getEmail() != null && u.getEmail().contains("." + paciente.getId() + "@")) {
                    pacienteUsuario = u;
                    break;
                }
            }
        }

        if (pacienteUsuario == null) {
            throw new RuntimeException("Usuário do paciente não encontrado");
        }

        // Busca os agendamentos ativos e agendados do paciente
        List<Agendamento> agendamentos = agendamentoService.buscarAtivosEAgendadosPorPaciente(pacienteUsuario.getId());
        if (agendamentos.isEmpty()) {
            throw new RuntimeException("Paciente não possui agendamentos ativos");
        }

        // Pega o último agendamento da série
        Agendamento ultimoAgendamento = agendamentos.stream()
                .max((a1, a2) -> a1.getData().compareTo(a2.getData()))
                .orElse(agendamentos.get(0));

        // Obtém o serieId do agendamento
        UUID serieId = ultimoAgendamento.getSerieId();

        // Obtém o número da última sessão
        int ultimaSessao = agendamentos.stream()
                .mapToInt(Agendamento::getNumeroSessao)
                .max()
                .orElse(0);

        // Obtém o total de sessões configurado para o paciente
        int totalSessoes = paciente.getTotalSessoes();

        // Usa o valor total de sessões configurado para o paciente como número de sessões a serem criadas
        // Isso garante que sempre serão criadas totalSessoes novas sessões, independente de quantas já existem
        int sessoesAdicionais = totalSessoes;

        System.out.println("Criando " + sessoesAdicionais + " novas sessões para o paciente.");

        // Obtém a data do último agendamento
        LocalDate dataUltimoAgendamento = ultimoAgendamento.getData();

        // Obtém o dia da semana do agendamento
        DayOfWeek diaSemana = dataUltimoAgendamento.getDayOfWeek();

        // Cria as sessões adicionais
        LocalDate dataAgendamento = dataUltimoAgendamento.plusDays(7); // Avança uma semana
        int agendamentosCriados = 0; // Contador para agendamentos criados com sucesso
        for (int i = 0; i < sessoesAdicionais; i++) {
            // Cria o agendamento
            Agendamento agendamento = new Agendamento();
            agendamento.setPsicologo(paciente.getPsicologo());
            agendamento.setPaciente(pacienteUsuario);
            agendamento.setSerieId(serieId);
            // Numera as novas sessões sequencialmente a partir da última sessão existente
            agendamento.setNumeroSessao(ultimaSessao + i + 1);
            agendamento.setData(dataAgendamento);
            agendamento.setHoraInicio(ultimoAgendamento.getHoraInicio());
            agendamento.setHoraFim(ultimoAgendamento.getHoraFim());
            agendamento.setStatus(Agendamento.StatusAgendamento.AGENDADO);
            agendamento.setAtivo(true);

            // Log antes de salvar o agendamento
            System.out.println("Tentando criar agendamento para a data: " + dataAgendamento + 
                               ", horário: " + agendamento.getHoraInicio() + "-" + agendamento.getHoraFim() + 
                               ", sessão número: " + agendamento.getNumeroSessao());

            try {
                // Salva o agendamento
                Agendamento agendamentoSalvo = agendamentoService.criar(agendamento);
                System.out.println("Agendamento criado com sucesso, ID: " + agendamentoSalvo.getId());
                agendamentosCriados++; // Incrementa o contador de agendamentos criados
            } catch (Exception e) {
                System.err.println("Erro ao criar agendamento: " + e.getMessage());
                // Continua para a próxima iteração em vez de interromper todo o processo
                continue;
            }

            // Avança para a próxima semana
            dataAgendamento = dataAgendamento.plusDays(7);
        }

        // Verifica se pelo menos um agendamento foi criado
        if (agendamentosCriados == 0) {
            System.err.println("Nenhum agendamento foi criado. Verifique se há conflitos de horário.");
            throw new RuntimeException("Não foi possível criar nenhum agendamento adicional. Verifique se há conflitos de horário.");
        }

        System.out.println("Total de agendamentos criados com sucesso: " + agendamentosCriados + " de " + sessoesAdicionais + " solicitados");
        return converterParaDTO(paciente);
    }

    /**
     * Converte uma entidade Paciente para DTO
     */
    private PacienteDTO converterParaDTO(Paciente paciente) {
        PacienteDTO dto = new PacienteDTO();
        dto.setId(paciente.getId());
        dto.setNome(paciente.getNome());
        dto.setSobrenome(paciente.getSobrenome());
        dto.setSexo(paciente.getSexo());
        dto.setDataNascimento(paciente.getDataNascimento());
        dto.setEmail(paciente.getEmail());
        dto.setTelefone(paciente.getTelefone());
        dto.setImagemBase64(paciente.getImagemBase64());
        dto.setImagemTipo(paciente.getImagemTipo());
        dto.setPsicologoId(paciente.getPsicologo().getId());
        dto.setTotalSessoes(paciente.getTotalSessoes());
        dto.setDataCriacao(paciente.getDataCriacao());
        dto.setAtivo(paciente.getAtivo());

        // Busca informações do agendamento atual
        try {
            // Busca o primeiro agendamento ativo e agendado do paciente
            Usuario pacienteUsuario = null;
            try {
                pacienteUsuario = usuarioService.buscarPorId(paciente.getId());
            } catch (Exception e) {
                // Tenta encontrar o usuário pelo email
                List<Usuario> usuarios = usuarioService.listarTodos();
                for (Usuario u : usuarios) {
                    if (u.getEmail() != null && u.getEmail().contains("." + paciente.getId() + "@")) {
                        pacienteUsuario = u;
                        break;
                    }
                }
            }

            if (pacienteUsuario != null) {
                List<Agendamento> agendamentos = agendamentoService.buscarAtivosEAgendadosPorPaciente(pacienteUsuario.getId());
                if (!agendamentos.isEmpty()) {
                    Agendamento agendamento = agendamentos.get(0);
                    dto.setSerieId(agendamento.getSerieId());
                    dto.setDiaSemana(agendamento.getData().getDayOfWeek());
                    dto.setDiaSemanaAntigo(agendamento.getData().getDayOfWeek());

                    // Busca o horário disponível correspondente
                    List<HorarioDisponivel> horarios = horarioDisponivelRepository.findByPsicologoAndDiaSemana(
                            paciente.getPsicologo(), agendamento.getData().getDayOfWeek());

                    for (HorarioDisponivel horario : horarios) {
                        if (horario.getHoraInicio().equals(agendamento.getHoraInicio()) && 
                            horario.getHoraFim().equals(agendamento.getHoraFim())) {
                            dto.setHorarioDisponivelId(horario.getId());
                            dto.setHorarioDisponivelIdAntigo(horario.getId());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignora erros ao buscar informações do agendamento
            System.err.println("Erro ao buscar informações do agendamento: " + e.getMessage());
        }

        return dto;
    }
}
