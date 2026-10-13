package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * One way out of the incident, as the proposal offers it (D46).
 *
 * <p>The {@code ReplanReason} behind it is not a field: it is the strategy the domain would build the
 * alternative with, and naming its kind would mean asking which one it is, the condition by node type that
 * D54 kept out of the adapter. What the client needs is the number, to accept it with, and the summary, which
 * is the domain's own wording of what the option does (D58).
 *
 * @param number the position in the list of options, from 1, which is what {@code POST .../acceptance} sends
 */
public record ReplanOptionDto(int number, String summary) {
}
